package io.github.kxyr0.echoplayeractor.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;
import io.github.kxyr0.echoplayeractor.forge.api.view.PlayerActorSnapshotView;
import io.github.kxyr0.echoplayeractor.forge.runtime.ServerPlayerActorHost;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerActorCommands {
    private final Supplier<ServerPlayerActorHost> host;
    private final PlayerActorOwnerId commandOwner = PlayerActorOwnerId.system("diagnostics");

    public PlayerActorCommands(Supplier<ServerPlayerActorHost> host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_player_actor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn_at")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("pitch",
                                                                        DoubleArgumentType.doubleArg())
                                                                        .executes(context -> spawnAt(
                                                                                context.getSource(),
                                                                                EntityArgument.getPlayer(context,
                                                                                        "player"),
                                                                                DoubleArgumentType.getDouble(context,
                                                                                        "x"),
                                                                                DoubleArgumentType.getDouble(context,
                                                                                        "y"),
                                                                                DoubleArgumentType.getDouble(context,
                                                                                        "z"),
                                                                                DoubleArgumentType.getDouble(context,
                                                                                        "yaw"),
                                                                                DoubleArgumentType.getDouble(context,
                                                                                        "pitch"))))))))))
                .then(Commands.literal("hotbar")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                                .then(Commands.argument("item_id", StringArgumentType.word())
                                                        .executes(context -> setHotbar(context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                IntegerArgumentType.getInteger(context, "slot"),
                                                                StringArgumentType.getString(context, "item_id"), 1))
                                                        .then(Commands.argument("count",
                                                                IntegerArgumentType.integer(1))
                                                                .executes(context -> setHotbar(context.getSource(),
                                                                        EntityArgument.getPlayer(context, "player"),
                                                                        IntegerArgumentType.getInteger(context,
                                                                                "slot"),
                                                                        StringArgumentType.getString(context,
                                                                                "item_id"),
                                                                        IntegerArgumentType.getInteger(context,
                                                                                "count"))))))))
                        .then(Commands.literal("select")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                                .executes(context -> selectHotbar(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "slot")))))))
                .then(Commands.literal("sit_at")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> sitAt(context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                IntegerArgumentType.getInteger(context, "x"),
                                                                IntegerArgumentType.getInteger(context, "y"),
                                                                IntegerArgumentType.getInteger(context, "z")))))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspect(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("opening")
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> openingStatus(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context.getSource())))
                .then(Commands.literal("control")
                        .then(Commands.literal("release")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> release(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))))));
    }

    private int spawnAt(CommandSourceStack source, ServerPlayer player, double x, double y, double z, double yaw,
            double pitch) {
        return send(source, host.get().spawnAt(new PlayerActorSpawnRequest(commandId(), actorId(player),
                commandOwner, new Vector3(x, y, z), yaw, pitch)));
    }

    private int setHotbar(CommandSourceStack source, ServerPlayer player, int slot, String itemId, int count) {
        return send(source, host.get().setHotbarItem(new PlayerHotbarItemRequest(commandId(), actorId(player),
                commandOwner, slot, itemId, count, false)));
    }

    private int selectHotbar(CommandSourceStack source, ServerPlayer player, int slot) {
        return send(source, host.get().selectHotbarSlot(new PlayerHotbarSelectionRequest(commandId(),
                actorId(player), commandOwner, slot)));
    }

    private int sitAt(CommandSourceStack source, ServerPlayer player, int x, int y, int z) {
        return send(source, host.get().sitAt(new PlayerSitRequest(commandId(), actorId(player), commandOwner,
                new BlockPosition(x, y, z), 40)));
    }

    private int inspect(CommandSourceStack source, ServerPlayer player) {
        Optional<PlayerActorSnapshotView> snapshot = host.get().snapshot(actorId(player));
        String status = snapshot.map(view -> "position=" + view.snapshot().transform().position()
                        + ", yaw=" + view.snapshot().transform().bodyYaw()
                        + ", pitch=" + view.snapshot().transform().pitch()
                        + ", sitting=" + view.snapshot().seat().seated()
                        + ", selectedHotbarSlot=" + host.get().selectedHotbarSlot(actorId(player)))
                .orElse("player actor unavailable");
        source.sendSuccess(() -> Component.literal(status), false);
        return Command.SINGLE_SUCCESS;
    }

    private int openingStatus(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("Opening state is owned by Echo Story Engine for "
                + player.getGameProfile().getName() + "; expected flags: chapter_1.opening_started, "
                + "chapter_1.opening_completed."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int diagnostics(CommandSourceStack source) {
        String codes = Arrays.stream(PlayerActorFailure.values())
                .filter(value -> value.name().startsWith("PLAYER_ACTOR_SPAWN")
                        || value.name().startsWith("PLAYER_ACTOR_HOTBAR")
                        || value.name().startsWith("PLAYER_ACTOR_ITEM")
                        || value.name().startsWith("PLAYER_ACTOR_SEAT")
                        || value.name().startsWith("PLAYER_ACTOR_SIT")
                        || value.name().startsWith("PLAYER_ACTOR_OPENING"))
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        source.sendSuccess(() -> Component.literal("PA0 diagnostics: " + codes), false);
        return Command.SINGLE_SUCCESS;
    }

    private int release(CommandSourceStack source, ServerPlayer player) {
        host.get().emergencyRelease(actorId(player));
        source.sendSuccess(() -> Component.literal("Echo Player Actor control released for "
                + player.getGameProfile().getName()), true);
        return 1;
    }

    private int send(CommandSourceStack source, PlayerActorResult result) {
        if (result.failure().isPresent()) {
            source.sendFailure(Component.literal(result.failure().orElseThrow() + ": " + result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.status() + " " + result.outcome()
                + ": " + result.message()), true);
        return Command.SINGLE_SUCCESS;
    }

    private PlayerActorCommandId commandId() {
        return PlayerActorCommandId.create();
    }

    private PlayerActorId actorId(ServerPlayer player) {
        return PlayerActorId.of(player.getUUID());
    }
}
