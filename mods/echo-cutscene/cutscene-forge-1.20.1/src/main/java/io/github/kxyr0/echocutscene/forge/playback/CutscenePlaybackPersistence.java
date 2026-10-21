package io.github.kxyr0.echocutscene.forge.playback;

import io.github.kxyr0.echocutscene.forge.playback.persistence.SavedCutscenePlayback;

import io.github.kxyr0.echocutscene.forge.playback.persistence.CutscenePlaybackSavedData;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneOwnerId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlayback;
import io.github.kxyr0.echocutscene.forge.body.BodyLockStore;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class CutscenePlaybackPersistence {
    private final ServerCutsceneResourceCatalog catalog;
    private final BodyLockStore bodyLocks;

    CutscenePlaybackPersistence(ServerCutsceneResourceCatalog catalog, BodyLockStore bodyLocks) {
        this.catalog = catalog;
        this.bodyLocks = bodyLocks;
    }

    void save(ServerPlayer player, ActiveCutscenePlayback playback) {
        Optional.ofNullable(player.getServer()).ifPresent(server -> save(server, player.getUUID(), playback));
    }

    void remove(ServerPlayer player) {
        Optional.ofNullable(player.getServer()).ifPresent(server -> remove(server, player.getUUID()));
    }

    Optional<ActiveCutscenePlayback> restore(ServerPlayer player) {
        Optional<ActiveCutscenePlayback> restored = Optional.ofNullable(player.getServer())
                .flatMap(server -> restore(server, player.getUUID()));
        if (restored.isEmpty()) {
            remove(player);
        }
        return restored;
    }

    Optional<ActiveCutscenePlayback> reattach(ServerPlayer player, Optional<ActiveCutscenePlayback> current) {
        return current.or(() -> restore(player)).map(playback -> {
            var waiting = playback.waiting(playback.elapsedMillis());
            save(player, waiting);
            return waiting;
        });
    }

    private void save(MinecraftServer server, UUID playerId, ActiveCutscenePlayback playback) {
        bodyLocks.snapshot(playerId).ifPresent(snapshot -> CutscenePlaybackSavedData.open(server)
                .put(new SavedCutscenePlayback(playerId, playback.playback().playbackId(),
                        playback.playback().definition().cutsceneId(),
                        playback.playback().definition().endBehavior(),
                        playback.playback().status(), playback.elapsedMillis(), snapshot)));
    }

    private void remove(MinecraftServer server, UUID playerId) {
        CutscenePlaybackSavedData.open(server).remove(playerId);
    }

    private Optional<ActiveCutscenePlayback> restore(MinecraftServer server, UUID playerId) {
        return CutscenePlaybackSavedData.open(server).find(playerId)
                .filter(saved -> !io.github.kxyr0.echocutscene.forge.domain.CutsceneDomainEvents.terminalCommitted(
                        server, saved.playbackId().value())).flatMap(this::restore);
    }

    private Optional<ActiveCutscenePlayback> restore(SavedCutscenePlayback saved) {
        Optional<CutsceneDefinition> definition = catalog.definition(saved.cutsceneId());
        Optional<CameraTrackSnapshot> track = definition.flatMap(value -> catalog.track(value.cameraTrackId()));
        if (definition.isEmpty() || track.isEmpty()) {
            return Optional.empty();
        }
        CutsceneDefinition playbackDefinition = definition.orElseThrow().withEndBehavior(saved.endBehavior());
        CutscenePlayback playback = new CutscenePlayback(saved.playbackId(),
                CutsceneOwnerId.player(saved.playerId()), playbackDefinition,
                Util.getMillis(), CutsceneStatus.WAITING_FOR_CLIENT);
        bodyLocks.restore(saved.playerId(), saved.playbackId(), saved.bodySnapshot());
        return Optional.of(new ActiveCutscenePlayback(playback, track.orElseThrow(), saved.elapsedMillis()));
    }
}
