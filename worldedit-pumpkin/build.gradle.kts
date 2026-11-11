import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

plugins {
    java
    id("io.github.udouble-factory.pumpkin") version libs.versions.pumpkinApi
}

dependencies {
    implementation("com.sk89q.worldedit:worldedit-core:${providers.gradleProperty("version").get()}") {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.mozilla")
        exclude(group = "org.antlr", module = "antlr4")
    }
    implementation("org.antlr:antlr4-runtime:${libs.versions.antlr.get()}")
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.fastutil)
    compileOnly("org.teavm:teavm-extension-spi:0.16.0")
    compileOnly(libs.jsr305)
    compileOnly(libs.autoValue.annotations)
}

val buildInfoDir = layout.buildDirectory.dir("generated/pumpkin-build")
val worldEditVersion = providers.gradleProperty("version")

val generatePumpkinBuild = tasks.register("generatePumpkinBuild") {
    val target = buildInfoDir

    inputs.property("version", worldEditVersion)
    outputs.dir(target)

    doLast {
        val file = target.get().file("com/sk89q/worldedit/pumpkin/PumpkinBuild.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.sk89q.worldedit.pumpkin;\n\n" +
                "public final class PumpkinBuild {\n" +
                "    public static final String VERSION = \"" + inputs.properties["version"] + "\";\n\n" +
                "    private PumpkinBuild() {\n    }\n}\n"
        )
    }
}

val coreMain = file("../worldedit-core/src/main")
val dataDir = layout.buildDirectory.dir("generated/pumpkin-data")

val generatePumpkinData = tasks.register("generatePumpkinData") {
    val itemsFile = File(coreMain, "resources/com/sk89q/worldedit/world/registry/items.json")
    val blockCategoriesFile = File(coreMain, "java/com/sk89q/worldedit/world/block/BlockCategories.java")
    val itemCategoriesFile = File(coreMain, "java/com/sk89q/worldedit/world/item/ItemCategories.java")
    val output = dataDir

    inputs.file(itemsFile)
    inputs.file(blockCategoriesFile)
    inputs.file(itemCategoriesFile)
    outputs.dir(output)

    doLast {
        val target = output.get().dir("pumpkin").asFile
        target.mkdirs()

        val itemId = Regex("\"id\":\"([^\"]+)\"")
        val categoryId = Regex("get\\(\"([^\"]+)\"\\)")

        File(target, "items.txt").writeText(itemId.findAll(itemsFile.readText()).map { it.groupValues[1] }.joinToString("\n"))
        File(target, "block-categories.txt").writeText(categoryId.findAll(blockCategoriesFile.readText()).map { it.groupValues[1] }.joinToString("\n"))
        File(target, "item-categories.txt").writeText(categoryId.findAll(itemCategoriesFile.readText()).map { it.groupValues[1] }.joinToString("\n"))
    }
}

val langDir = layout.buildDirectory.dir("generated/pumpkin-lang")

val generatePumpkinLang = tasks.register("generatePumpkinLang") {
    val classpath: FileCollection = configurations.runtimeClasspath.get()
    val output = langDir

    inputs.files(classpath)
    outputs.dir(output)

    doLast {
        val target = output.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        val archive = classpath.files.firstNotNullOfOrNull { entry ->
            when {
                entry.isDirectory -> File(entry, "lang/i18n.zip").takeIf { it.isFile }?.readBytes()
                entry.name.endsWith(".jar") -> ZipFile(entry).use { jar ->
                    jar.getEntry("lang/i18n.zip")?.let { jar.getInputStream(it).readBytes() }
                }
                else -> null
            }
        } ?: throw GradleException("lang/i18n.zip was not found on the runtime classpath")

        val locales = sortedSetOf<String>()

        ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val locale = entry.name.substringBefore('/')
                if (entry.isDirectory || entry.name != "$locale/strings.json") {
                    continue
                }

                val bytes = zip.readBytes()
                if (bytes.size < 16) {
                    continue
                }

                val file = File(target, "lang/$locale/strings.json")
                file.parentFile.mkdirs()
                file.writeBytes(bytes)
                locales.add(locale)
            }
        }

        val index = File(target, "pumpkin/locales.txt")
        index.parentFile.mkdirs()
        index.writeText(locales.joinToString("\n"))
    }
}

abstract class OverrideTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val overrides: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val additions: DirectoryProperty

    @get:Internal
    abstract val coreSources: DirectoryProperty

    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:Internal
    abstract val lockFile: RegularFileProperty

    protected fun sources(root: File): List<String> {
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    protected fun upstream(path: String): String? {
        val source = coreSources.get().file(path).asFile
        if (source.isFile) {
            return hash(source.readText().replace("\r\n", "\n").toByteArray())
        }

        val entry = path.removeSuffix(".java") + ".class"

        for (library in libraries.files) {
            if (library.isDirectory) {
                val file = File(library, entry)
                if (file.isFile) {
                    return hash(file.readBytes())
                }
            } else if (library.name.endsWith(".jar")) {
                ZipFile(library).use { jar ->
                    val found = jar.getEntry(entry)
                    if (found != null) {
                        return hash(jar.getInputStream(found).readBytes())
                    }
                }
            }
        }

        return null
    }

    protected fun misplaced(): List<String> {
        val problems = mutableListOf<String>()

        for (path in sources(overrides.get().asFile)) {
            if (upstream(path) == null) {
                problems.add("src/overrides/java/$path replaces nothing: its upstream class is gone, so delete it or move it to src/additions/java")
            }
        }

        for (path in sources(additions.get().asFile)) {
            if (upstream(path) != null) {
                problems.add("src/additions/java/$path now hides an upstream class: move it to src/overrides/java")
            }
        }

        return problems
    }

    private fun hash(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

abstract class CheckOverrides : OverrideTask() {
    @TaskAction
    fun check() {
        val problems = misplaced().toMutableList()
        val lock = lockFile.get().asFile
        val recorded = if (lock.isFile) lock.readLines().filter { it.contains('=') }.associate { it.substringBefore('=') to it.substringAfter('=') } else emptyMap()
        val present = sources(overrides.get().asFile)

        for (path in present) {
            val now = upstream(path) ?: continue
            val before = recorded[path]

            if (before == null) {
                problems.add("src/overrides/java/$path is not recorded in overrides.lock")
            } else if (before != now) {
                problems.add("src/overrides/java/$path: its upstream class changed since the override was last reviewed")
            }
        }

        for (path in recorded.keys - present.toSet()) {
            problems.add("overrides.lock lists $path, which is no longer an override")
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Replaced upstream classes need attention:\n  " + problems.joinToString("\n  ") +
                    "\nCompare each file with its upstream class, bring the override up to date, then run the lockOverrides task."
            )
        }
    }
}

abstract class LockOverrides : OverrideTask() {
    @TaskAction
    fun lock() {
        val problems = misplaced()
        if (problems.isNotEmpty()) {
            throw GradleException("Replaced upstream classes need attention:\n  " + problems.joinToString("\n  "))
        }

        val lines = sources(overrides.get().asFile).map { "$it=${upstream(it)}" }
        lockFile.get().asFile.writeText(lines.joinToString("\n", postfix = "\n"))
    }
}

tasks.withType<OverrideTask>().configureEach {
    overrides.set(layout.projectDirectory.dir("src/overrides/java"))
    additions.set(layout.projectDirectory.dir("src/additions/java"))
    coreSources.set(layout.projectDirectory.dir("../worldedit-core/src/main/java"))
    libraries.from(configurations.runtimeClasspath)
    lockFile.set(layout.projectDirectory.file("overrides.lock"))
}

val checkOverrides = tasks.register<CheckOverrides>("checkOverrides")

tasks.register<LockOverrides>("lockOverrides")

sourceSets.main {
    java.srcDir("src/overrides/java")
    java.srcDir("src/additions/java")
    java.srcDir(generatePumpkinBuild)
    resources.srcDir(generatePumpkinData)
    resources.srcDir(generatePumpkinLang)
}

tasks.compileJava {
    dependsOn(checkOverrides)
    options.release.set(libs.versions.primaryJavaRelease.get().toInt())
    options.encoding = "UTF-8"
}

pumpkin {
    apiGroup.set("com.github.UDouble-Factory.pumpkin-api-java")
    apiVersion.set(libs.versions.pumpkinApi)
    pluginClass.set("com.sk89q.worldedit.pumpkin.PumpkinWorldEdit")
    maxHeapMb.set(1536)
}
