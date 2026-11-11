/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.pumpkin;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.command.CommandUtil;
import com.sk89q.worldedit.internal.event.InteractionDebouncer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemCategory;
import com.sk89q.worldedit.world.item.ItemType;
import org.apache.logging.log4j.LogManager;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.part.CommandPart;
import org.enginehub.piston.part.SubCommandPart;
import plugin.PluginMetadata;
import plugin.PumpkinPlugin;
import pumpkin.biomes.Biome;
import pumpkin.command.Arg;
import pumpkin.command.ArgumentType;
import pumpkin.command.CommandError;
import pumpkin.command.CommandNode;
import pumpkin.command.CommandSender;
import pumpkin.command.CommandSuggestion;
import pumpkin.command.CommandSuggestions;
import pumpkin.command.ConsumedArgs;
import pumpkin.command.StringType;
import pumpkin.command.SuggestionRequest;
import pumpkin.common.BlockPos;
import pumpkin.context.Context;
import pumpkin.event.Event;
import pumpkin.event.EventPriority;
import pumpkin.event.EventType;
import pumpkin.event.InteractAction;
import pumpkin.event.PlayerCustomPayloadEventData;
import pumpkin.event.PlayerInteractEventData;
import pumpkin.permission.Permission;
import pumpkin.permission.PermissionDefault;
import pumpkin.permission.PermissionLevel;
import pumpkin.player.Player;
import pumpkin.runtime.Option;
import pumpkin.runtime.Result;
import pumpkin.server.Server;
import pumpkin.world.World;
import pumpkin.world.WorldApi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PumpkinWorldEdit extends PumpkinPlugin {
    private static final String NAME = "worldedit";
    private static final String COMMAND_PERMISSION = "worldedit:command";
    private static final long EVENT_INTERACT = 1;
    private static final long EVENT_LEAVE = 2;
    private static final long EVENT_PAYLOAD = 3;
    private static final long FIRST_COMMAND = 16;

    private final Map<Long, String> commandNames = new HashMap<>();
    private final Map<UUID, PumpkinPlayer> players = new HashMap<>();
    private final Map<String, PumpkinWorld> worlds = new HashMap<>();
    private final Set<String> permissions = new HashSet<>();
    private final Path dataDirectory = Paths.get("data");
    private long nextCommand = FIRST_COMMAND;
    private boolean loading;
    private boolean unloading;
    private Context context;
    private Server server;
    private PumpkinPlatform platform;
    private PumpkinConfiguration configuration;
    private InteractionDebouncer debouncer;

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(NAME, PumpkinBuild.VERSION, List.of("EngineHub"), "In-game world editor", List.of(), List.of("fs.write.data"));
    }

    @Override
    public Result<Void, String> onLoad(Context context) {
        try {
            this.context = context;
            loading = true;
            server = context.getServer();
            configuration = new PumpkinConfiguration(dataDirectory);
            platform = new PumpkinPlatform(this);
            debouncer = new InteractionDebouncer(platform);

            if (permissions.add(COMMAND_PERMISSION)) {
                context.registerPermission(new Permission(COMMAND_PERMISSION, "", new PermissionDefault.Allow(), List.of()));
            }

            WorldEdit worldEdit = WorldEdit.getInstance();
            worldEdit.getPlatformManager().register(platform);
            worldEdit.getEventBus().post(new PlatformsRegisteredEvent());

            registerTypes();
            configuration.load();
            worldEdit.getEventBus().post(new ConfigurationLoadEvent(configuration));

            worldEdit.getEventBus().post(new PlatformReadyEvent(platform));

            context.registerEvent(EVENT_INTERACT, EventType.PLAYER_INTERACT_EVENT, EventPriority.NORMAL, true);
            context.registerEvent(EVENT_LEAVE, EventType.PLAYER_LEAVE_EVENT, EventPriority.NORMAL, false);
            context.registerEvent(EVENT_PAYLOAD, EventType.PLAYER_CUSTOM_PAYLOAD_EVENT, EventPriority.NORMAL, false);
            scheduleTimers();
        } catch (RuntimeException | Error failure) {
            LogManager.getLogger().error("WorldEdit could not be loaded", failure);
            throw failure;
        } finally {
            loading = false;
        }

        return Result.success(null);
    }

    @Override
    public Result<Void, String> onUnload(Context context) {
        try (context) {
            unloading = true;

            WorldEdit worldEdit = WorldEdit.getInstance();
            worldEdit.getSessionManager().unload();
            worldEdit.getEventBus().post(new PlatformUnreadyEvent(platform));
            worldEdit.getPlatformManager().unregister(platform);
        }

        return Result.success(null);
    }

    private void registerTypes() {
        PumpkinBlockRegistry blocks = platform.registries().blocks();

        for (String name : WorldApi.getAllBlockNames()) {
            String id = PumpkinAdapter.key(name);
            if (BlockType.REGISTRY.get(id) == null) {
                BlockType.REGISTRY.register(id, new BlockType(id, state -> applyDefaults(blocks, id, state)));
            }
        }

        for (String id : PumpkinData.lines("items")) {
            if (ItemType.REGISTRY.get(id) == null) {
                ItemType.REGISTRY.register(id, new ItemType(id));
            }
        }

        for (Biome biome : Biome.values()) {
            String id = PumpkinAdapter.enumKey(biome);
            if (BiomeType.REGISTRY.get(id) == null) {
                BiomeType.REGISTRY.register(id, new BiomeType(id));
            }
        }

        for (String id : PumpkinData.lines("block-categories")) {
            if (BlockCategory.REGISTRY.get(id) == null) {
                BlockCategory.REGISTRY.register(id, new BlockCategory(id));
            }
        }

        for (String id : PumpkinData.lines("item-categories")) {
            if (ItemCategory.REGISTRY.get(id) == null) {
                ItemCategory.REGISTRY.register(id, new ItemCategory(id));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockState applyDefaults(PumpkinBlockRegistry blocks, String id, BlockState state) {
        BlockState result = state;

        for (Map.Entry<String, String> entry : blocks.defaultProperties(id).entrySet()) {
            com.sk89q.worldedit.registry.state.Property property = state.getBlockType().getPropertyMap().get(entry.getKey());
            if (property != null) {
                result = result.with(property, property.getValueFor(entry.getValue()));
            }
        }

        return result;
    }

    void registerPermission(String node) {
        if (permissions.add(node)) {
            context.registerPermission(new Permission(node, "", new PermissionDefault.Op(PermissionLevel.TWO), List.of()));
        }
    }

    void registerCommands(CommandManager manager) {
        if (!loading) {
            return;
        }

        Locale locale = WorldEdit.getInstance().getConfiguration().defaultLocale;

        manager.getAllCommands().forEach(command -> {
            collectPermissions(command);

            long handler = nextCommand++;
            commandNames.put(handler, command.getName());

            List<String> names = new ArrayList<>();
            names.add(command.getName());
            names.addAll(command.getAliases());

            pumpkin.command.Command root = pumpkin.command.Command.create(names, WorldEditText.reduceToText(command.getDescription(), locale));
            CommandNode arguments = CommandNode.argument("args", new ArgumentType.String(StringType.GREEDY));
            arguments.executeWithHandlerId(handler);
            root.then(arguments);
            root.executeWithHandlerId(handler);
            context.registerCommand(root, COMMAND_PERMISSION);
        });
    }

    private void collectPermissions(Command command) {
        command.getCondition().as(com.sk89q.worldedit.command.util.PermissionCondition.class).ifPresent(condition -> {
            for (String node : condition.getPermissions()) {
                registerPermission(PumpkinAdapter.permission(node));
            }
        });

        for (CommandPart part : command.getParts()) {
            if (part instanceof SubCommandPart sub) {
                for (Command child : sub.getCommands()) {
                    collectPermissions(child);
                }
            }
        }
    }

    @Override
    public Result<Integer, CommandError> handleCommand(long handlerId, CommandSender sender, Server callServer, ConsumedArgs args) {
        try (sender; callServer; args) {
            String name = commandNames.get(handlerId);
            if (name == null) {
                return Result.success(0);
            }

            String arguments = argument(args.getValue("args"));
            String line = arguments.isEmpty() ? "/" + name : "/" + name + " " + arguments;
            WorldEdit.getInstance().getEventBus().post(new CommandEvent(actor(sender), line));
            return Result.success(1);
        }
    }

    @Override
    public CommandSuggestions handleCommandSuggestion(long handlerId, CommandSender sender, Server callServer, SuggestionRequest request) {
        try (sender; callServer) {
            String name = commandNames.get(handlerId);
            String remaining = request.remaining();
            int word = remaining.lastIndexOf(' ') + 1;
            long start = request.start() + word;
            long length = remaining.length() - word;
            List<CommandSuggestion> result = new ArrayList<>();

            if (name != null) {
                String line = "/" + name + " " + remaining;
                CommandSuggestionEvent event = new CommandSuggestionEvent(actor(sender), line);
                WorldEdit.getInstance().getEventBus().post(event);

                for (String suggestion : CommandUtil.fixSuggestions(line, event.getSuggestions())) {
                    result.add(new CommandSuggestion(suggestion, Option.none()));
                }
            }

            return new CommandSuggestions(start, length, result);
        }
    }

    private static String argument(Arg arg) {
        if (arg instanceof Arg.Simple simple) {
            return simple.value();
        }
        if (arg instanceof Arg.Msg message) {
            return message.value();
        }

        return "";
    }

    private Actor actor(CommandSender sender) {
        Option<Player> player = sender.asPlayer();
        if (player.isSome()) {
            return player(player.value());
        }

        return new PumpkinCommandSender(sender);
    }

    @Override
    public Event handleEvent(long handlerId, Server callServer, Event event) {
        try (callServer) {
            if (handlerId == EVENT_INTERACT && event instanceof Event.PlayerInteractEvent interact) {
                return handleInteract(interact);
            }
            if (handlerId == EVENT_LEAVE && event instanceof Event.PlayerLeaveEvent leave) {
                PumpkinPlayer player = players.remove(PumpkinAdapter.uuid(leave.value().player().getId()));
                if (player != null) {
                    debouncer.clearInteraction(player);
                    player.disconnect();
                    WorldEdit.getInstance().getEventBus().post(new SessionIdleEvent(player.getSessionKey()));
                }
            }
            if (handlerId == EVENT_PAYLOAD && event instanceof Event.PlayerCustomPayloadEvent payload) {
                handlePayload(payload.value());
            }

            return event;
        }
    }

    private Event handleInteract(Event.PlayerInteractEvent event) {
        PlayerInteractEventData data = event.value();
        PumpkinPlayer player = player(PumpkinAdapter.uuid(data.player().getId()));
        if (player == null) {
            return event;
        }

        InteractAction action = data.action();

        if (action != InteractAction.LEFT_CLICK_BLOCK) {
            Optional<Boolean> previous = debouncer.getDuplicateInteractionResult(player);
            if (previous.isPresent()) {
                return previous.get() ? cancelled(data) : event;
            }
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        boolean left = action == InteractAction.LEFT_CLICK_BLOCK || action == InteractAction.LEFT_CLICK_AIR;
        boolean cancel = false;

        if (action == InteractAction.LEFT_CLICK_BLOCK || action == InteractAction.RIGHT_CLICK_BLOCK) {
            Option<BlockPos> clicked = data.clickedPos();
            if (clicked.isSome()) {
                BlockPos pos = clicked.value();
                Location location = new Location(player.getWorld(), BlockVector3.at(pos.x(), pos.y(), pos.z()).toVector3());

                cancel = left ? worldEdit.handleBlockLeftClick(player, location, null) : worldEdit.handleBlockRightClick(player, location, null);
            }
        }

        if (!cancel) {
            cancel = left ? worldEdit.handleArmSwing(player) : worldEdit.handleRightClick(player);
        }

        debouncer.setLastInteraction(player, cancel);
        return cancel ? cancelled(data) : event;
    }

    private void handlePayload(PlayerCustomPayloadEventData data) {
        if (!PumpkinPlayer.CUI_CHANNEL.equals(data.channel())) {
            return;
        }

        PumpkinPlayer player = player(PumpkinAdapter.uuid(data.player().getId()));
        if (player == null) {
            return;
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        session.handleCUIInitializationMessage(new String(data.data(), StandardCharsets.UTF_8), player);
    }

    private static Event cancelled(PlayerInteractEventData data) {
        return new Event.PlayerInteractEvent(new PlayerInteractEventData(data.player(), data.action(), data.clickedPos(), data.block(), true));
    }

    PumpkinPlayer player(Player handle) {
        UUID id = PumpkinAdapter.uuid(handle.getId());
        PumpkinPlayer existing = players.get(id);
        if (existing != null) {
            existing.replace(handle);
            return existing;
        }

        PumpkinPlayer created = new PumpkinPlayer(this, handle);
        players.put(id, created);
        return created;
    }

    PumpkinPlayer player(UUID id) {
        PumpkinPlayer existing = players.get(id);
        if (existing != null) {
            return existing;
        }

        Option<Player> found = server.getPlayerByUuid(PumpkinAdapter.uuid(id));
        if (!found.isSome()) {
            return null;
        }

        PumpkinPlayer created = new PumpkinPlayer(this, found.value());
        players.put(id, created);
        return created;
    }

    boolean isOnline(UUID id) {
        return unloading || players.containsKey(id);
    }

    Collection<Actor> connectedUsers() {
        List<Actor> result = new ArrayList<>();

        for (Player handle : server.getAllPlayers()) {
            result.add(player(handle));
        }

        return result;
    }

    PumpkinWorld world(World handle) {
        String name = handle.getName();
        PumpkinWorld existing = worlds.get(name);
        if (existing != null) {
            handle.close();
            return existing;
        }

        PumpkinWorld created = new PumpkinWorld(this, handle);
        worlds.put(name, created);
        return created;
    }

    Option<World> openWorld(String name) {
        return server.getWorldByName(name);
    }

    List<PumpkinWorld> worlds() {
        List<PumpkinWorld> result = new ArrayList<>();

        for (World handle : server.getAllWorlds()) {
            result.add(world(handle));
        }

        return result;
    }

    int schedule(long delay, long period, Runnable task) {
        tasks().afterTicks(Math.max(1, delay), callServer -> {
            try (callServer) {
                task.run();
            }

            if (period > 0) {
                schedule(period, period, task);
            }
        });

        return 0;
    }

    private void scheduleTimers() {
        tasks().afterTicks(20, callServer -> {
            try (callServer) {
                PumpkinTimers.runDue();
            }

            scheduleTimers();
        });
    }

    Path dataDirectory() {
        return dataDirectory;
    }

    String serverVersion() {
        return server.getSysInfo().pumpkinVersion();
    }

    PumpkinConfiguration configuration() {
        return configuration;
    }
}
