package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.Optional;

/**
 * Durable idempotency ledger for play and stop commands.
 */
public final class AnimationCommandLedgerFlow {
    public Optional<AnimationPlayResult> replayPlay(ReplayPlay request) {
        CommandRecord record = request.data().commandLedger.get(request.commandId());
        if (record == null) {
            return Optional.empty();
        }
        if (!record.argumentsFingerprint.equals(request.argumentsFingerprint())) {
            return Optional.of(new AnimationPlayResult(AnimationPlayResult.Status.IDEMPOTENCY_CONFLICT,
                    request.animationId(), request.subjectId(), Optional.empty(), Optional.empty(),
                    "EANIM-A3-IDEMPOTENCY", "Command id was reused with different arguments."));
        }
        return Optional.of(new AnimationPlayResult(AnimationPlayResult.Status.valueOf(record.status),
                request.animationId(), request.subjectId(), record.playbackId,
                record.playbackId.isPresent() ? Optional.of(AnimationPhase.ENTER) : Optional.empty(),
                record.diagnosticCode, record.message));
    }

    public Optional<AnimationStopResult> replayStop(ReplayStop request) {
        CommandRecord record = request.data().commandLedger.get(request.commandId());
        if (record == null) {
            return Optional.empty();
        }
        if (!record.argumentsFingerprint.equals(request.argumentsFingerprint())) {
            return Optional.of(new AnimationStopResult(AnimationStopResult.Status.IDEMPOTENCY_CONFLICT,
                    request.subjectId(), Optional.empty(), "EANIM-A3-IDEMPOTENCY",
                    "Command id was reused with different arguments."));
        }
        return Optional.of(new AnimationStopResult(AnimationStopResult.Status.valueOf(record.status),
                request.subjectId(), record.playbackId, record.diagnosticCode, record.message));
    }

    public void remember(AnimationRuntimeSavedData data, RememberCommand command) {
        CommandRecord record = new CommandRecord();
        record.commandId = command.commandId();
        record.operation = command.operation();
        record.argumentsFingerprint = command.argumentsFingerprint();
        record.status = command.status();
        record.playbackId = command.playbackId();
        record.diagnosticCode = command.diagnosticCode();
        record.message = command.message();
        data.commandLedger.put(command.commandId(), record);
        data.setDirtyRevision();
    }

    public record ReplayPlay(
            AnimationRuntimeSavedData data,
            AnimationCommandId commandId,
            String argumentsFingerprint,
            AnimationId animationId,
            AnimationSubjectId subjectId
    ) {
    }

    public record ReplayStop(
            AnimationRuntimeSavedData data,
            AnimationCommandId commandId,
            String argumentsFingerprint,
            AnimationSubjectId subjectId
    ) {
    }

    public record RememberCommand(
            AnimationCommandId commandId,
            String operation,
            String argumentsFingerprint,
            String status,
            Optional<AnimationPlaybackId> playbackId,
            String diagnosticCode,
            String message
    ) {
    }
}
