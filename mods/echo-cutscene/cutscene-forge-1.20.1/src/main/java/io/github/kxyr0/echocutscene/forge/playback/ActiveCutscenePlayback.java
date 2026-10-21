package io.github.kxyr0.echocutscene.forge.playback;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlayback;
import io.github.kxyr0.echocutscene.forge.api.service.CutscenePlaybackView;
import io.github.kxyr0.echocutscene.forge.network.ClientboundCutsceneStartPacket;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;

record ActiveCutscenePlayback(
        CutscenePlayback playback,
        CameraTrackSnapshot track,
        long elapsedOffsetMillis
) {
    java.util.Optional<ActiveCutscenePlayback> atEnd() {
        if (waitsForClient() || elapsedMillis() < track.metadata().durationMillis()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(playback.definition().endBehavior()
                == io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior.HOLD_LAST_FRAME
                ? held(track.metadata().durationMillis()) : this);
    }
    ActiveCutscenePlayback waiting(long elapsedMillis) {
        return new ActiveCutscenePlayback(playback.status(CutsceneStatus.WAITING_FOR_CLIENT),
                track, elapsedMillis);
    }

    ActiveCutscenePlayback prepared(long nowMillis) {
        return new ActiveCutscenePlayback(playback.startedAt(nowMillis).status(CutsceneStatus.PLAYING),
                track, elapsedOffsetMillis);
    }

    ActiveCutscenePlayback held(long elapsedMillis) {
        return new ActiveCutscenePlayback(playback.status(CutsceneStatus.HOLDING_LAST_FRAME),
                track, elapsedMillis);
    }

    long elapsedMillis() {
        long elapsed = rawElapsedMillis();
        return Math.min(track.metadata().durationMillis(), Math.max(0L, elapsed));
    }

    boolean waitsForClient() {
        return playback.status() == CutsceneStatus.WAITING_FOR_CLIENT
                || playback.status() == CutsceneStatus.STARTING
                || playback.status() == CutsceneStatus.HOLDING_LAST_FRAME;
    }

    ClientboundCutsceneStartPacket startPacket() {
        return new ClientboundCutsceneStartPacket(playback.playbackId(), track, elapsedMillis(),
                playback.definition().speed(), playback.definition().endBehavior(),
                playback.definition().hudPolicy());
    }

    CutscenePlaybackView view(ServerPlayer player) {
        return new CutscenePlaybackView(playback.playbackId(), playback.definition().cutsceneId(),
                playback.definition().cameraTrackId(), player.getUUID(), playback.status(),
                elapsedMillis(), playback.definition().speed());
    }

    private long rawElapsedMillis() {
        if (waitsForClient()) {
            return elapsedOffsetMillis;
        }
        long liveElapsed = Math.round((Util.getMillis() - playback.startedAtMillis())
                * playback.definition().speed());
        return elapsedOffsetMillis + liveElapsed;
    }
}
