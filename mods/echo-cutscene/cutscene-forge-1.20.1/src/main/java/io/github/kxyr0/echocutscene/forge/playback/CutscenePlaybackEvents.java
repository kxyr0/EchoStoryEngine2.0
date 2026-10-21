package io.github.kxyr0.echocutscene.forge.playback;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneDiagnostic;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.core.lifecycle.CutscenePlayback;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneLifecycleEvent;
import io.github.kxyr0.echocutscene.forge.service.CutsceneDiagnosticsLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class CutscenePlaybackEvents {
    private CutscenePlaybackEvents() {
    }

    static CutsceneResult failure(CutsceneDiagnosticsLedger diagnostics, String code, String message) {
        diagnostics.record(CutsceneDiagnostic.of(code, message));
        return CutsceneResult.failed(code, message);
    }

    static CutsceneStatus status(CutsceneOutcome outcome) {
        return outcome == CutsceneOutcome.STOPPED ? CutsceneStatus.STOPPED : CutsceneStatus.COMPLETED;
    }

    static void post(ServerPlayer player, CutscenePlayback playback, CutsceneOutcome outcome) {
        io.github.kxyr0.echocutscene.forge.domain.CutsceneDomainEvents.publish(player, playback, outcome);
        MinecraftForge.EVENT_BUS.post(new CutsceneLifecycleEvent(player, playback.playbackId(),
                playback.status(), outcome));
    }
}
