package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.Optional;

/**
 * Applies public play/stop commands with idempotency ledger support.
 */
public final class AnimationPlaybackCommandFlow {
    private final AnimationCommandLedgerFlow commandLedger = new AnimationCommandLedgerFlow();
    private final AnimationPlaybackStartFlow startFlow = new AnimationPlaybackStartFlow();
    private final AnimationPlaybackStopFlow stopFlow = new AnimationPlaybackStopFlow();

    public PlayResult play(PlayRequest request) {
        String fingerprint = "play|" + request.animationId() + "|" + request.subjectId();
        var replay = commandLedger.replayPlay(new AnimationCommandLedgerFlow.ReplayPlay(request.data(),
                request.commandId(), fingerprint, request.animationId(), request.subjectId()));
        if (replay.isPresent()) {
            return new PlayResult(replay.orElseThrow(), Optional.empty());
        }
        var started = startFlow.start(new AnimationPlaybackStartFlow.StartRequest(request.data(),
                request.animationId(), request.subjectId(), request.commandId(), request.definition(),
                request.serverTick(), request.playbackSpeed()));
        remember(request.commandId(), "play", fingerprint, started.result(), request.data());
        return new PlayResult(started.result(), started.changedSubject());
    }

    public StopResult stop(StopRequest request) {
        String fingerprint = "stop|" + request.subjectId() + "|"
                + request.animationId().map(AnimationId::value).orElse("*");
        var replay = commandLedger.replayStop(new AnimationCommandLedgerFlow.ReplayStop(request.data(),
                request.commandId(), fingerprint, request.subjectId()));
        if (replay.isPresent()) {
            return new StopResult(replay.orElseThrow(), Optional.empty());
        }
        var stopped = stopFlow.stop(request.data(), request.subjectId(), request.animationId());
        remember(request.commandId(), "stop", fingerprint, stopped.result(), request.data());
        return new StopResult(stopped.result(), stopped.changedSubject());
    }

    private void remember(AnimationCommandId commandId, String operation, String fingerprint,
            AnimationPlayResult result, AnimationRuntimeSavedData data) {
        commandLedger.remember(data, new AnimationCommandLedgerFlow.RememberCommand(commandId, operation, fingerprint,
                result.status().name(), result.playbackId(), result.diagnosticCode(), result.message()));
    }

    private void remember(AnimationCommandId commandId, String operation, String fingerprint,
            AnimationStopResult result, AnimationRuntimeSavedData data) {
        commandLedger.remember(data, new AnimationCommandLedgerFlow.RememberCommand(commandId, operation, fingerprint,
                result.status().name(), result.playbackId(), result.diagnosticCode(), result.message()));
    }

    public record PlayRequest(
            AnimationRuntimeSavedData data,
            AnimationId animationId,
            AnimationSubjectId subjectId,
            AnimationCommandId commandId,
            Optional<AnimationDefinition> definition,
            long serverTick,
            double playbackSpeed
    ) {
    }

    public record StopRequest(
            AnimationRuntimeSavedData data,
            AnimationSubjectId subjectId,
            Optional<AnimationId> animationId,
            AnimationCommandId commandId
    ) {
    }

    public record PlayResult(AnimationPlayResult result, Optional<AnimationSubjectId> changedSubject) {
    }

    public record StopResult(AnimationStopResult result, Optional<AnimationSubjectId> changedSubject) {
    }
}
