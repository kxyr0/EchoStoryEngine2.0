package io.github.kxyr0.echonpc.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

/**
 * Registers the explicit N1 /echo_npc command tree.
 */
public final class EchoNpcCommands {
    private final EchoNpcCommandActions actions;
    private final EchoNpcSceneCommandActions sceneActions;
    private final EchoNpcFormCommandActions formActions;
    private final EchoNpcSkinCommandActions skinActions;

    public EchoNpcCommands(
            NpcCatalogState catalogState,
            InMemoryDiagnosticBuffer diagnostics,
            Supplier<Optional<ForgeNpcHost>> host
    ) {
        this.actions = new EchoNpcCommandActions(catalogState, diagnostics, Objects.requireNonNull(host, "host"));
        this.sceneActions = new EchoNpcSceneCommandActions(host);
        this.formActions = new EchoNpcFormCommandActions(host);
        this.skinActions = new EchoNpcSkinCommandActions(host);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_npc").requires(source -> source.hasPermission(2))
                .then(Commands.literal("health").executes(actions::health))
                .then(Commands.literal("list").executes(actions::list))
                .then(Commands.literal("definitions").executes(actions::definitions))
                .then(Commands.literal("actors").executes(actions::actors))
                .then(Commands.literal("actor").then(Commands.argument("actorId", ResourceLocationArgument.id())
                        .executes(actions::actor)))
                .then(spawn())
                .then(Commands.literal("teleport").then(Commands.argument("actorId", ResourceLocationArgument.id())
                        .then(Commands.argument("pos", Vec3Argument.vec3()).executes(actions::teleport))))
                .then(Commands.literal("despawn").then(Commands.argument("actorId", ResourceLocationArgument.id())
                        .executes(actions::despawn)))
                .then(forms())
                .then(form())
                .then(skin())
                .then(spawnAt())
                .then(placeAt())
                .then(sleepAt())
                .then(Commands.literal("wake_up").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .executes(sceneActions::wakeUp)))
                .then(Commands.literal("inspect_rest").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .executes(sceneActions::inspectRest)))
                .then(Commands.literal("inspect_bed").then(Commands.argument("bed", BlockPosArgument.blockPos())
                        .executes(sceneActions::inspectBed)))
                .then(Commands.literal("rest_providers").executes(sceneActions::restProviders))
                .then(Commands.literal("diagnostics").executes(actions::diagnostics)
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                                .executes(actions::diagnostics)))
                .then(Commands.literal("operations").executes(actions::operations))
                .then(Commands.literal("operation").then(Commands.argument("operationId", StringArgumentType.word())
                        .executes(actions::operation)))
                .then(Commands.literal("blocked").executes(actions::blocked))
                .then(Commands.literal("recovery").then(Commands.literal("retry")
                        .then(Commands.argument("actorId", ResourceLocationArgument.id())
                                .executes(actions::recoveryRetry))))
                .then(Commands.literal("persistence").then(Commands.literal("status")
                        .executes(actions::persistenceStatus))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> spawn() {
        return Commands.literal("spawn")
                .then(Commands.argument("actorId", ResourceLocationArgument.id())
                        .then(Commands.argument("definitionId", ResourceLocationArgument.id())
                                .executes(actions::spawnHere)
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(actions::spawnAt))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> spawnAt() {
        return Commands.literal("spawn_at")
                .then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                                                .executes(sceneActions::spawnAt)))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> placeAt() {
        return Commands.literal("place_at")
                .then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                                                .executes(sceneActions::placeAt)))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> forms() {
        return Commands.literal("forms")
                .then(Commands.literal("list").executes(formActions::list))
                .then(Commands.literal("inspect").then(Commands.argument("formId", ResourceLocationArgument.id())
                        .executes(formActions::inspect)));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> form() {
        return Commands.literal("form")
                .then(Commands.literal("get").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .executes(formActions::get)))
                .then(Commands.literal("set").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .then(Commands.argument("formId", ResourceLocationArgument.id())
                                .executes(formActions::set))))
                .then(Commands.literal("validate_space")
                        .then(Commands.argument("npcId", ResourceLocationArgument.id())
                                .then(Commands.argument("formId", ResourceLocationArgument.id())
                                        .executes(formActions::validateSpace))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> skin() {
        return Commands.literal("skin")
                .then(Commands.literal("get").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .executes(skinActions::get)))
                .then(Commands.literal("set").then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .then(Commands.argument("skinId", ResourceLocationArgument.id())
                                .executes(skinActions::set))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> sleepAt() {
        return Commands.literal("sleep_at")
                .then(Commands.argument("npcId", ResourceLocationArgument.id())
                        .then(Commands.argument("bed", BlockPosArgument.blockPos())
                                .then(Commands.argument("expectedBlock", ResourceLocationArgument.id())
                                        .executes(sceneActions::sleepAt))));
    }
}
