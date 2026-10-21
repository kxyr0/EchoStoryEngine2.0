package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.List;
import java.util.Optional;

/**
 * Persists graceful stop requests without cancelling the current pose.
 */
public final class AnimationPlaybackStopFlow {
    public StopFlowResult stop(AnimationRuntimeSavedData data, AnimationSubjectId subjectId,
            Optional<AnimationId> animationId) {
        if (!data.subjects.containsKey(subjectId)) {
            return rejected(AnimationStopResult.Status.SUBJECT_NOT_FOUND, subjectId, "EANIM-A3-SUBJECT",
                    "Animation subject not found: " + subjectId);
        }
        List<PlaybackRecord> active = active(data, subjectId, animationId);
        if (active.isEmpty()) {
            return rejected(AnimationStopResult.Status.PLAYBACK_NOT_FOUND, subjectId, "EANIM-A3-NO-PLAYBACK",
                    "No matching active animation.");
        }
        if (active.size() > 1) {
            return rejected(AnimationStopResult.Status.AMBIGUOUS_ACTIVE_ANIMATION, subjectId, "EANIM-A3-AMBIGUOUS",
                    "More than one active animation matches.");
        }
        PlaybackRecord playback = active.get(0);
        playback.exitRequested = true;
        playback.playbackRevision++;
        data.setDirtyRevision();
        return new StopFlowResult(new AnimationStopResult(AnimationStopResult.Status.STOP_REQUESTED, subjectId,
                Optional.of(playback.playbackId), "EANIM-A3-STOP", "Graceful stop requested."),
                Optional.of(subjectId));
    }

    private List<PlaybackRecord> active(
            AnimationRuntimeSavedData data,
            AnimationSubjectId subjectId,
            Optional<AnimationId> animationId
    ) {
        return data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .filter(playback -> animationId.isEmpty() || playback.animationId.equals(animationId.orElseThrow()))
                .toList();
    }

    private StopFlowResult rejected(
            AnimationStopResult.Status status,
            AnimationSubjectId subjectId,
            String code,
            String message
    ) {
        return new StopFlowResult(new AnimationStopResult(status, subjectId, Optional.empty(), code, message),
                Optional.empty());
    }

    public record StopFlowResult(AnimationStopResult result, Optional<AnimationSubjectId> changedSubject) {
    }
}
