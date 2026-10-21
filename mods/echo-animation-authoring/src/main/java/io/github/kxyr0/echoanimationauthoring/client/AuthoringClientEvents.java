package io.github.kxyr0.echoanimationauthoring.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimationauthoring.editor.AuthoringLabels;
import io.github.kxyr0.echoanimationauthoring.editor.EditorSession;
import io.github.kxyr0.echoanimationauthoring.draft.DefaultAnimationAuthoringService;
import io.github.kxyr0.echoanimationauthoring.forge.api.AnimationAuthoringServiceLookupEvent;
import io.github.kxyr0.echoanimationauthoring.migration.LegacyFaceMigration;
import io.github.kxyr0.echoanimationauthoring.migration.MigrationResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * Registers client-side development commands for the authoring surface.
 */
public final class AuthoringClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORKSPACE_PROPERTY = "echoAnimationAuthoring.workspace";
    private static final List<String> DIAGNOSTICS = new ArrayList<>();
    private static final DefaultAnimationAuthoringService AUTHORING =
            new DefaultAnimationAuthoringService(AuthoringClientEvents::configuredWorkspace);

    private AuthoringClientEvents() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(AuthoringClientEvents::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(AuthoringClientEvents::onServiceLookup);
    }

    private static void onServiceLookup(AnimationAuthoringServiceLookupEvent event) {
        event.provide(AUTHORING);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_animation_authoring")
                .executes(context -> openEditor())
                .then(Commands.literal("editor").executes(context -> openEditor()))
                .then(Commands.literal("workspace").executes(context -> reportWorkspace(context.getSource())))
                .then(Commands.literal("face")
                        .then(Commands.literal("open").executes(context -> openEditor())
                                .then(Commands.argument("faceRigId", StringArgumentType.greedyString())
                                        .executes(context -> openFace(context.getSource(),
                                                StringArgumentType.getString(context, "faceRigId"))))))
                .then(Commands.literal("migrate")
                        .then(Commands.argument("legacyPath", StringArgumentType.greedyString())
                                .executes(context -> migrate(context.getSource(),
                                        StringArgumentType.getString(context, "legacyPath")))))
                .then(Commands.literal("diagnostics").executes(context -> diagnostics(context.getSource(), 20))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                .executes(context -> diagnostics(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "limit"))))));
    }

    private static int openEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new AuthoringScreen(session(AuthoringLabels.TRANSLATE))));
        return Command.SINGLE_SUCCESS;
    }

    private static int reportWorkspace(CommandSourceStack source) {
        return configuredWorkspace()
                .map(workspace -> {
                    success(source, "Echo Animation Authoring workspace: " + workspace);
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    diagnostic("EANIM-AUTH-WORKSPACE", AuthoringLabels.WORKSPACE_NOT_CONFIGURED);
                    failure(source, AuthoringLabels.WORKSPACE_NOT_CONFIGURED + "; задайте JVM property "
                            + WORKSPACE_PROPERTY + ".");
                    return 0;
                });
    }

    private static int openFace(CommandSourceStack source, String faceRigId) {
        success(source, "Открыт face rig: " + faceRigId);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new AuthoringScreen(session("FaceRig " + faceRigId))));
        return Command.SINGLE_SUCCESS;
    }

    private static int migrate(CommandSourceStack source, String rawPath) {
        Path input = Path.of(rawPath).toAbsolutePath().normalize();
        try {
            String legacy = Files.readString(input, StandardCharsets.UTF_8);
            MigrationResult result = new LegacyFaceMigration().migrate(legacy, animationId(input), "jdh:john_father");
            success(source, "Миграция завершена: " + input + ", EchoScript chars=" + result.echoScript().length()
                    + ", warnings=" + result.warnings().size());
            result.warnings().forEach(warning -> diagnostic("EANIM-AUTH-MIGRATION", warning));
            LOGGER.info("EANIM-AUTH-MIGRATION source={} output=\n{}", input, result.echoScript());
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            diagnostic("EANIM-AUTH-MIGRATION", "Миграция не выполнена: " + exception.getMessage());
            failure(source, "Миграция не выполнена: " + exception.getMessage());
            return 0;
        }
    }

    private static int diagnostics(CommandSourceStack source, int limit) {
        List<String> snapshot;
        synchronized (DIAGNOSTICS) {
            snapshot = List.copyOf(DIAGNOSTICS);
        }
        if (snapshot.isEmpty()) {
            success(source, "Echo Animation Authoring diagnostics: none");
            return 0;
        }
        snapshot.stream()
                .skip(Math.max(0, snapshot.size() - limit))
                .forEach(message -> success(source, message));
        return Math.min(limit, snapshot.size());
    }

    private static EditorSession session(String selectedTool) {
        return new EditorSession(configuredWorkspace(), selectedTool, false, 0);
    }

    private static java.util.Optional<Path> configuredWorkspace() {
        String raw = System.getProperty(WORKSPACE_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            return java.util.Optional.empty();
        }
        Path path = Path.of(raw).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            diagnostic("EANIM-AUTH-WORKSPACE", "Workspace invalid: " + path);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(path);
    }

    private static String animationId(Path input) {
        String fileName = input.getFileName().toString();
        int dot = fileName.indexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        return "jdh:migrated/" + stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]+", "_");
    }

    private static void diagnostic(String code, String message) {
        String full = code + " " + message;
        synchronized (DIAGNOSTICS) {
            DIAGNOSTICS.add(full);
            if (DIAGNOSTICS.size() > 100) {
                DIAGNOSTICS.remove(0);
            }
        }
        LOGGER.warn(full);
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
    }
}
