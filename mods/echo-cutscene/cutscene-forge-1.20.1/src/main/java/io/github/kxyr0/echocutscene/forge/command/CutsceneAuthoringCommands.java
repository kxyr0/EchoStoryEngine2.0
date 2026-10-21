package io.github.kxyr0.echocutscene.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetBinding;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.authoring.ServerCutsceneAuthoringHost;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class CutsceneAuthoringCommands {
    private final ServerCutsceneAuthoringHost authoring;

    CutsceneAuthoringCommands(ServerCutsceneAuthoringHost authoring) {
        this.authoring = authoring;
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root().then(enter()).then(mode()).then(target()).then(exit()).then(author()));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("echo_cutscene").requires(source -> source.hasPermission(2));
    }

    private LiteralArgumentBuilder<CommandSourceStack> author() {
        return Commands.literal("author").then(enter()).then(mode()).then(target()).then(exit()).then(status());
    }

    private LiteralArgumentBuilder<CommandSourceStack> enter() {
        return Commands.literal("enter").then(Commands.argument("id", ResourceLocationArgument.id())
                .executes(context -> enter(context.getSource(),
                        ResourceLocationArgument.getId(context, "id").toString(), 0.28D))
                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.01D, 5.0D))
                        .executes(context -> enter(context.getSource(),
                                ResourceLocationArgument.getId(context, "id").toString(),
                                DoubleArgumentType.getDouble(context, "speed")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> mode() {
        return Commands.literal("mode").then(Commands.argument("mode", StringArgumentType.word())
                .executes(context -> mode(context.getSource(),
                        StringArgumentType.getString(context, "mode"))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> target() {
        return Commands.literal("target")
                .then(Commands.literal("clear").executes(context -> clearTarget(context.getSource())))
                .then(Commands.literal("player")
                        .executes(context -> targetPlayer(context.getSource(), "EYES"))
                        .then(Commands.argument("anchor", StringArgumentType.word())
                                .executes(context -> targetPlayer(context.getSource(),
                                        StringArgumentType.getString(context, "anchor")))))
                .then(Commands.literal("fixed").then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(context -> targetFixed(context.getSource(),
                                Vec3Argument.getVec3(context, "pos")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> exit() {
        return Commands.literal("exit").executes(context -> exit(context.getSource()));
    }

    private LiteralArgumentBuilder<CommandSourceStack> status() {
        return Commands.literal("status").executes(context -> status(context.getSource()));
    }

    private int enter(CommandSourceStack source, String id, double cameraSpeed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return report(source, authoring.enter(player, CutsceneId.parse(id), CutsceneCommandId.create(),
                cameraSpeed));
    }

    private int mode(CommandSourceStack source, String mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Optional<CameraControlMode> parsed = mode(mode);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("Unknown camera mode: " + mode));
            return 0;
        }
        return report(source, authoring.setMode(source.getPlayerOrException(),
                parsed.orElseThrow(), CutsceneCommandId.create()));
    }

    private int targetPlayer(CommandSourceStack source, String anchor)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Optional<CameraTargetAnchor> parsed = anchor(anchor);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("Unknown camera target anchor: " + anchor));
            return 0;
        }
        CameraTarget target = CameraTarget.player(parsed.orElseThrow(), CameraTargetBinding.LIVE);
        return report(source, authoring.setTarget(source.getPlayerOrException(), target,
                CutsceneCommandId.create()));
    }

    private int targetFixed(CommandSourceStack source, Vec3 position)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CameraTarget target = CameraTarget.fixed(new Vector3(position.x, position.y, position.z));
        return report(source, authoring.setTarget(source.getPlayerOrException(), target,
                CutsceneCommandId.create()));
    }

    private int clearTarget(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return report(source, authoring.clearTarget(source.getPlayerOrException(), CutsceneCommandId.create()));
    }

    private int exit(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return report(source, authoring.exit(source.getPlayerOrException(),
                CutsceneId.parse("jdh:authoring"), CutsceneCommandId.create()));
    }

    private int status(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        authoring.activeCutscene(source.getPlayerOrException()).ifPresentOrElse(cutscene ->
                source.sendSuccess(() -> Component.literal("cutscene authoring=" + cutscene), false),
                () -> source.sendSuccess(() -> Component.literal("cutscene authoring=none"), false));
        return 1;
    }

    private static Optional<CameraControlMode> mode(String value) {
        try {
            return Optional.of(CameraControlMode.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<CameraTargetAnchor> anchor(String value) {
        try {
            return Optional.of(CameraTargetAnchor.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static int report(CommandSourceStack source, CutsceneResult result) {
        source.sendSuccess(() -> Component.literal("cutscene authoring outcome=" + result.outcome()
                + " playback=" + result.playbackId().map(Object::toString).orElse("none")), false);
        return result.diagnostics().isEmpty() ? 1 : 0;
    }
}
