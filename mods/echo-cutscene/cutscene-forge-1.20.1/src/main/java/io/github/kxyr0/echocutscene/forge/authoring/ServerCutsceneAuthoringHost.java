package io.github.kxyr0.echocutscene.forge.authoring;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.mode.CameraControlMode;
import io.github.kxyr0.echocutscene.api.camera.target.CameraTarget;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.core.lifecycle.CommandIdempotencyLedger;
import io.github.kxyr0.echocutscene.core.recording.CameraSourceWriter;
import io.github.kxyr0.echocutscene.forge.body.BodyLockStore;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import io.github.kxyr0.echocutscene.forge.playback.CutsceneResourceLoader;
import io.github.kxyr0.echocutscene.forge.playback.FileSystemTrackWatcher;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutsceneResourceCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerCutsceneAuthoringHost {
    private final BodyLockStore bodyLocks;
    private final ServerCutsceneResourceCatalog catalog;
    private final CutsceneResourceLoader loader;
    private final FileSystemTrackWatcher watcher;
    private final CameraSourceWriter writer = new CameraSourceWriter();
    private final CommandIdempotencyLedger commands = new CommandIdempotencyLedger();
    private final Map<UUID, AuthoringSession> active = new HashMap<>();

    public ServerCutsceneAuthoringHost(BodyLockStore bodyLocks, ServerCutsceneResourceCatalog catalog,
            CutsceneResourceLoader loader, FileSystemTrackWatcher watcher) {
        this.bodyLocks = bodyLocks;
        this.catalog = catalog;
        this.loader = loader;
        this.watcher = watcher;
    }

    public CutsceneResult enter(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return enter(player, cutsceneId, commandId, 0.28D);
    }

    public CutsceneResult enter(
            ServerPlayer player,
            CutsceneId cutsceneId,
            CutsceneCommandId commandId,
            double cameraSpeed
    ) {
        Optional<CutsceneResult> previous = commands.previous(commandId);
        if (previous.isPresent()) {
            return previous.orElseThrow();
        }
        return commands.remember(commandId, startNew(player, cutsceneId, cameraSpeed));
    }

    public CutsceneResult exit(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId,
                finish(player, CutsceneOutcome.STOPPED)));
    }

    public Optional<CutsceneId> activeCutscene(ServerPlayer player) {
        return Optional.ofNullable(active.get(player.getUUID())).map(AuthoringSession::cutsceneId);
    }

    public void close(ServerPlayer player) {
        finish(player, CutsceneOutcome.STOPPED);
    }

    public CutsceneResult setMode(ServerPlayer player, CameraControlMode mode, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId, mode(player, mode)));
    }

    public CutsceneResult setTarget(ServerPlayer player, CameraTarget target, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId, target(player, target)));
    }

    public CutsceneResult clearTarget(ServerPlayer player, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId, clearTarget(player)));
    }

    public boolean save(ServerPlayer player, String sessionId, CutsceneId cutsceneId, String source) {
        AuthoringSession session = active.get(player.getUUID());
        if (session == null || !session.sessionId().equals(sessionId) || !session.cutsceneId().equals(cutsceneId)) {
            player.sendSystemMessage(Component.literal("cutscene recording rejected: inactive authoring session"));
            return false;
        }
        return writeSource(player, cutsceneId, source);
    }

    private CutsceneResult mode(ServerPlayer player, CameraControlMode mode) {
        AuthoringSession session = active.get(player.getUUID());
        if (session == null) {
            return CutsceneResult.failed("CUTSCENE_AUTHORING_NOT_ACTIVE", "No authoring session for player.");
        }
        EchoCutsceneNetwork.authoringMode(player, session.sessionId(), mode);
        return CutsceneResult.accepted(session.playbackId());
    }

    private CutsceneResult target(ServerPlayer player, CameraTarget target) {
        AuthoringSession session = active.get(player.getUUID());
        if (session == null) {
            return CutsceneResult.failed("CUTSCENE_AUTHORING_NOT_ACTIVE", "No authoring session for player.");
        }
        EchoCutsceneNetwork.authoringTarget(player, session.sessionId(), Optional.of(target));
        return CutsceneResult.accepted(session.playbackId());
    }

    private CutsceneResult clearTarget(ServerPlayer player) {
        AuthoringSession session = active.get(player.getUUID());
        if (session == null) {
            return CutsceneResult.failed("CUTSCENE_AUTHORING_NOT_ACTIVE", "No authoring session for player.");
        }
        EchoCutsceneNetwork.authoringTarget(player, session.sessionId(), Optional.empty());
        return CutsceneResult.accepted(session.playbackId());
    }

    private CutsceneResult startNew(ServerPlayer player, CutsceneId cutsceneId, double cameraSpeed) {
        if (active.containsKey(player.getUUID())) {
            return alreadyAuthoring(player);
        }
        if (bodyLocks.locked(player)) {
            return CutsceneResult.failed("CUTSCENE_BODY_LOCKED", "Player body is already locked.");
        }
        Optional<String> idError = CameraAuthoringSourceGuard.validateCutsceneId(cutsceneId);
        if (idError.isPresent()) {
            return CutsceneResult.failed("CUTSCENE_ID_INVALID", idError.orElseThrow());
        }
        if (cameraSpeed < 0.01D || cameraSpeed > 5.0D || !Double.isFinite(cameraSpeed)) {
            return CutsceneResult.failed("CUTSCENE_CAMERA_SPEED_INVALID", "Camera speed must be in [0.01, 5.0].");
        }
        AuthoringSession session = session(cutsceneId);
        active.put(player.getUUID(), session);
        bodyLocks.lock(player, session.playbackId());
        EchoCutsceneNetwork.authoringStart(player, session.sessionId(), cutsceneId, initialPose(player), cameraSpeed);
        return CutsceneResult.accepted(session.playbackId());
    }

    private CutsceneResult alreadyAuthoring(ServerPlayer player) {
        AuthoringSession session = active.get(player.getUUID());
        return new CutsceneResult(CutsceneOutcome.ALREADY_PLAYING, Optional.of(session.playbackId()),
                java.util.List.of());
    }

    private CutsceneResult finish(ServerPlayer player, CutsceneOutcome outcome) {
        AuthoringSession session = active.remove(player.getUUID());
        if (session == null) {
            return CutsceneResult.failed("CUTSCENE_AUTHORING_NOT_ACTIVE", "No authoring session for player.");
        }
        bodyLocks.unlock(player);
        EchoCutsceneNetwork.authoringStop(player, session.sessionId());
        return new CutsceneResult(outcome, Optional.of(session.playbackId()), java.util.List.of());
    }

    private boolean writeSource(ServerPlayer player, CutsceneId cutsceneId, String source) {
        Optional<String> validationError = CameraAuthoringSourceGuard.validateSource(source);
        if (validationError.isPresent()) {
            player.sendSystemMessage(Component.literal("cutscene recording rejected: "
                    + validationError.orElseThrow()));
            return false;
        }
        return writeAndReload(player, cutsceneId, source);
    }

    private boolean writeAndReload(ServerPlayer player, CutsceneId cutsceneId, String source) {
        MinecraftServer server = player.server;
        try {
            Path root = watcher.root(server);
            Path target = CameraAuthoringSourceGuard.target(root, cutsceneId);
            writer.writeAtomic(target, source);
            CutsceneResourceLoader.LoadedCutscenes loaded = loader.load(root);
            catalog.replaceAll(loaded.definitions(), loaded.tracks());
            player.sendSystemMessage(Component.literal("cutscene camera saved: " + cutsceneId));
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal("cutscene recording save failed: " + exception.getMessage()));
            return false;
        }
    }

    private static AuthoringSession session(CutsceneId cutsceneId) {
        String sessionId = UUID.randomUUID().toString();
        return new AuthoringSession(sessionId, cutsceneId, new CutscenePlaybackId("authoring-" + sessionId));
    }

    private static CameraPose initialPose(ServerPlayer player) {
        return new CameraPose(CameraPosition.of(player.getX(), player.getEyeY(), player.getZ()),
                new CameraRotation(player.getYRot(), player.getXRot(), 0.0D), CameraFieldOfView.normal());
    }

    private record AuthoringSession(String sessionId, CutsceneId cutsceneId, CutscenePlaybackId playbackId) {
    }
}
