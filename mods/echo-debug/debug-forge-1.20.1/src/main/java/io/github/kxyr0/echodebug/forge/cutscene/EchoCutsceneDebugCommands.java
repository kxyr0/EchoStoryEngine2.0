package io.github.kxyr0.echodebug.forge.cutscene;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetAnchor;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTargetBinding;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneAuthoringServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneAuthoringService;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public final class EchoCutsceneDebugCommands {
    private static final CutsceneId PREVIEW = CutsceneId.parse("jdh:chapter_1/bedtime_story");

    private EchoCutsceneDebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_debug").requires(source -> source.hasPermission(2))
                .then(Commands.literal("cutscene")
                        .then(Commands.literal("enter")
                                .executes(context -> enter(context.getSource(), PREVIEW.value(), 0.28D))
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(context -> enter(context.getSource(),
                                                ResourceLocationArgument.getId(context, "id").toString(), 0.28D))
                                        .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.01D, 5.0D))
                                                .executes(context -> enter(context.getSource(),
                                                        ResourceLocationArgument.getId(context, "id").toString(),
                                                        DoubleArgumentType.getDouble(context, "speed"))))))
                        .then(Commands.literal("mode")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(context -> mode(context.getSource(),
                                                StringArgumentType.getString(context, "mode")))))
                        .then(Commands.literal("target")
                                .then(Commands.literal("clear").executes(context -> clearTarget(context.getSource())))
                                .then(Commands.literal("player")
                                        .executes(context -> targetPlayer(context.getSource(), "EYES"))
                                        .then(Commands.argument("anchor", StringArgumentType.word())
                                                .executes(context -> targetPlayer(context.getSource(),
                                                        StringArgumentType.getString(context, "anchor")))))
                                .then(Commands.literal("fixed")
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(context -> targetFixed(context.getSource(),
                                                        Vec3Argument.getVec3(context, "pos"))))))
                        .then(Commands.literal("exit").executes(context -> exit(context.getSource())))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))));
    }

    private static int enter(CommandSourceStack source, String id, double cameraSpeed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        if (service.isEmpty()) {
            return unavailable(source);
        }
        return report(source, service.orElseThrow().enter(player, CutsceneId.parse(id), commandId("enter"),
                cameraSpeed));
    }

    private static int mode(CommandSourceStack source, String mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CameraControlMode> parsed = mode(mode);
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("Unknown camera mode: " + mode));
            return 0;
        }
        return service.map(value -> report(source, value.setMode(player, parsed.orElseThrow(), commandId("mode"))))
                .orElseGet(() -> unavailable(source));
    }

    private static int targetPlayer(CommandSourceStack source, String anchor)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CameraTargetAnchor> parsed = anchor(anchor);
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        if (parsed.isEmpty()) {
            source.sendFailure(Component.literal("Unknown camera target anchor: " + anchor));
            return 0;
        }
        CameraTarget target = CameraTarget.player(parsed.orElseThrow(), CameraTargetBinding.LIVE);
        return service.map(value -> report(source, value.setTarget(player, target, commandId("target"))))
                .orElseGet(() -> unavailable(source));
    }

    private static int targetFixed(CommandSourceStack source, Vec3 position)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        CameraTarget target = CameraTarget.fixed(new Vector3(position.x, position.y, position.z));
        return service.map(value -> report(source, value.setTarget(player, target, commandId("target"))))
                .orElseGet(() -> unavailable(source));
    }

    private static int clearTarget(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        return service.map(value -> report(source, value.clearTarget(player, commandId("target-clear"))))
                .orElseGet(() -> unavailable(source));
    }

    private static int exit(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        if (service.isEmpty()) {
            return unavailable(source);
        }
        return report(source, service.orElseThrow().exit(player, PREVIEW, commandId("exit")));
    }

    private static int status(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CutsceneAuthoringService> service = service(source.getServer());
        if (service.isEmpty()) {
            return unavailable(source);
        }
        boolean authoring = service.orElseThrow().isAuthoring(player);
        source.sendSuccess(() -> Component.literal("Echo Debug cutscene authoring=" + authoring), false);
        return Command.SINGLE_SUCCESS;
    }

    private static Optional<CutsceneAuthoringService> service(MinecraftServer server) {
        CutsceneAuthoringServiceLookupEvent event = new CutsceneAuthoringServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }

    private static CutsceneCommandId commandId(String action) {
        return new CutsceneCommandId("echo-debug:" + action + ":" + UUID.randomUUID());
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
        source.sendSuccess(() -> Component.literal("Echo Debug cutscene outcome=" + result.outcome()
                + " session=" + result.playbackId().map(Object::toString).orElse("none")), false);
        return result.diagnostics().isEmpty() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int unavailable(CommandSourceStack source) {
        source.sendFailure(Component.literal("Echo Cutscene service is unavailable."));
        return 0;
    }
}
