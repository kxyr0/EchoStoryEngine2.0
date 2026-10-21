package io.github.kxyr0.echocutscene.forge.playback;

import static io.github.kxyr0.echocutscene.forge.playback.CutscenePlaybackEvents.failure;
import static io.github.kxyr0.echocutscene.forge.playback.CutscenePlaybackEvents.post;
import static io.github.kxyr0.echocutscene.forge.playback.CutscenePlaybackEvents.status;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.lifecycle.CommandIdempotencyLedger;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlayback;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlaybackStarter;
import io.github.kxyr0.echocutscene.forge.api.service.CutscenePlaybackView;
import io.github.kxyr0.echocutscene.forge.body.BodyLockStore;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import io.github.kxyr0.echocutscene.forge.service.CutsceneDiagnosticsLedger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerCutscenePlaybackHost {
    private final ServerCutsceneResourceCatalog catalog;
    private final BodyLockStore bodyLocks;
    private final CutsceneDiagnosticsLedger diagnostics;
    private final CommandIdempotencyLedger commands = new CommandIdempotencyLedger();
    private final CutscenePlaybackStarter starter = new CutscenePlaybackStarter();
    private final Map<UUID, ActiveCutscenePlayback> active = new HashMap<>();
    private final CutscenePlaybackPersistence persistence;

    public ServerCutscenePlaybackHost(ServerCutsceneResourceCatalog catalog, BodyLockStore bodyLocks,
            CutsceneDiagnosticsLedger diagnostics) {
        this.catalog = catalog;
        this.bodyLocks = bodyLocks;
        this.diagnostics = diagnostics;
        persistence = new CutscenePlaybackPersistence(catalog, bodyLocks);
    }

    public CutsceneResult start(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return start(player, cutsceneId, commandId, CutsceneEndBehavior.AUTO_STOP);
    }

    public CutsceneResult start(
            ServerPlayer player,
            CutsceneId cutsceneId,
            CutsceneCommandId commandId,
            CutsceneEndBehavior endBehavior
    ) {
        Optional<CutsceneResult> previous = commands.previous(commandId);
        if (previous.isPresent()) {
            return previous.orElseThrow();
        }
        return commands.remember(commandId, startNew(player, cutsceneId, commandId, endBehavior));
    }

    public CutsceneResult stop(ServerPlayer player, CutsceneId cutsceneId, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId,
                stopMatching(player, cutsceneId)));
    }

    public CutsceneResult stopActive(ServerPlayer player, CutsceneCommandId commandId) {
        return commands.previous(commandId).orElseGet(() -> commands.remember(commandId,
                finish(player, CutsceneOutcome.STOPPED)));
    }

    public Optional<CutscenePlaybackView> view(ServerPlayer player) {
        return Optional.ofNullable(active.get(player.getUUID())).map(playback -> playback.view(player));
    }

    public void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(this::tickPlayer);
    }

    public void clearRuntime() { active.clear(); bodyLocks.clear(); }

    public void suspend(ServerPlayer player) {
        ActiveCutscenePlayback playback = active.get(player.getUUID());
        if (playback == null) {
            return;
        }
        ActiveCutscenePlayback suspended = playback.waiting(playback.elapsedMillis());
        active.put(player.getUUID(), suspended);
        persistence.save(player, suspended);
    }

    public void suspendAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(this::suspend);
    }

    public void reattach(ServerPlayer player) {
        persistence.reattach(player, Optional.ofNullable(active.get(player.getUUID()))).ifPresent(waiting -> {
            active.put(player.getUUID(), waiting);
            sendStart(player, waiting);
        });
    }

    private CutsceneResult startNew(ServerPlayer player, CutsceneId cutsceneId,
            CutsceneCommandId commandId, CutsceneEndBehavior endBehavior) {
        if (active.containsKey(player.getUUID())) {
            return new CutsceneResult(CutsceneOutcome.ALREADY_PLAYING,
                    Optional.of(active.get(player.getUUID()).playback().playbackId()), java.util.List.of());
        }
        if (bodyLocks.locked(player)) {
            return failure(diagnostics, "CUTSCENE_BODY_LOCKED", "Player body is already locked.");
        }
        Optional<CutsceneDefinition> definition = catalog.definition(cutsceneId);
        Optional<CameraTrackSnapshot> track = definition.flatMap(value -> catalog.track(value.cameraTrackId()));
        if (definition.isEmpty() || track.isEmpty()) {
            return failure(diagnostics, "CUTSCENE_TRACK_NOT_FOUND", "Cutscene track was not found.");
        }
        return startAccepted(player, definition.orElseThrow().withEndBehavior(endBehavior),
                track.orElseThrow(), commandId);
    }

    public void markPrepared(ServerPlayer player, CutscenePlaybackId playbackId) {
        ActiveCutscenePlayback activePlayback = active.get(player.getUUID());
        if (activePlayback == null || !activePlayback.playback().playbackId().equals(playbackId)) {
            return;
        }
        if (activePlayback.playback().status() == CutsceneStatus.PLAYING
                || activePlayback.playback().status() == CutsceneStatus.HOLDING_LAST_FRAME) {
            return;
        }
        ActiveCutscenePlayback prepared = activePlayback.prepared(Util.getMillis());
        active.put(player.getUUID(), prepared);
        persistence.save(player, prepared);
        post(player, prepared.playback(), CutsceneOutcome.ACCEPTED);
    }

    public void setBlack(ServerPlayer player, double canvasGroup, long timeMillis) {
        EchoCutsceneNetwork.setBlack(player, canvasGroup, timeMillis);
    }

    private CutsceneResult startAccepted(
            ServerPlayer player,
            CutsceneDefinition playbackDefinition,
            CameraTrackSnapshot track,
            CutsceneCommandId commandId
    ) {
        CutscenePlayback playback = starter.start(CutsceneOwnerId.player(player.getUUID()),
                playbackDefinition, Util.getMillis()).status(CutsceneStatus.WAITING_FOR_CLIENT);
        io.github.kxyr0.echocutscene.forge.domain.CutsceneDomainEvents.inheritCommand(player.getServer(),
                playback.playbackId().value(), commandId.value());
        ActiveCutscenePlayback activePlayback = new ActiveCutscenePlayback(playback, track, 0L);
        active.put(player.getUUID(), activePlayback);
        bodyLocks.lock(player, playback.playbackId());
        persistence.save(player, activePlayback);
        sendStart(player, activePlayback);
        post(player, playback, CutsceneOutcome.ACCEPTED);
        return CutsceneResult.accepted(playback.playbackId());
    }

    private void tickPlayer(ServerPlayer player) {
        Optional.ofNullable(active.get(player.getUUID())).flatMap(ActiveCutscenePlayback::atEnd)
                .ifPresent(playback -> {
                    if (playback.playback().status() == CutsceneStatus.HOLDING_LAST_FRAME) {
                        active.put(player.getUUID(), playback);
                        persistence.save(player, playback);
                    } else {
                        finish(player, CutsceneOutcome.COMPLETED);
                    }
                });
    }

    private CutsceneResult stopMatching(ServerPlayer player, CutsceneId cutsceneId) {
        ActiveCutscenePlayback playback = active.get(player.getUUID());
        if (playback == null) {
            return failure(diagnostics, "CUTSCENE_NOT_PLAYING", "No active cutscene for player.");
        }
        if (!playback.playback().definition().cutsceneId().equals(cutsceneId)) {
            return failure(diagnostics, "CUTSCENE_ID_MISMATCH", "Active cutscene is "
                    + playback.playback().definition().cutsceneId() + ", not " + cutsceneId + ".");
        }
        return finish(player, CutsceneOutcome.STOPPED);
    }

    private CutsceneResult finish(ServerPlayer player, CutsceneOutcome outcome) {
        ActiveCutscenePlayback playback = active.remove(player.getUUID());
        if (playback == null) {
            return failure(diagnostics, "CUTSCENE_NOT_PLAYING", "No active cutscene for player.");
        }
        bodyLocks.unlock(player);
        persistence.remove(player);
        EchoCutsceneNetwork.stop(player, playback.playback().playbackId());
        post(player, playback.playback().status(status(outcome)), outcome);
        return new CutsceneResult(outcome, Optional.of(playback.playback().playbackId()), java.util.List.of());
    }

    private void sendStart(ServerPlayer player, ActiveCutscenePlayback playback) {
        EchoCutsceneNetwork.start(player, playback.startPacket());
    }
}
