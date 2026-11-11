pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.UDouble-Factory.pumpkin-api-java")
            }
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.udouble-factory.pumpkin") {
                useModule("com.github.UDouble-Factory.pumpkin-api-java:gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
        mavenCentral()
        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.UDouble-Factory.pumpkin-api-java")
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "worldedit-pumpkin"

includeBuild("..")
