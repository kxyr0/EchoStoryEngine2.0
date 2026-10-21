package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Read-only public playback status query for Story wait/stop adapters.
 */
public final class AnimationPlaybackQueryFlow {
    public static AnimationPlaybackQueryResult unavailable(AnimationId animationId, AnimationSubjectId subjectId) {
        return new AnimationPlaybackQueryResult(AnimationPlaybackQueryResult.Status.DEFINITION_UNAVAILABLE,
                animationId, subjectId, Optional.empty(), Optional.empty(), Optional.empty(), 0L, 0L,
                "EANIM-A5-UNAVAILABLE", "Echo Animation playback query service is unavailable.");
    }

    public AnimationPlaybackQueryResult query(
            AnimationRuntimeSavedData data,
            List<AnimationDefinition> definitions,
            AnimationSubjectId subjectId,
            AnimationId animationId
    ) {
        SubjectRecord subject = data.subjects.get(subjectId);
        if (subject == null) {
            return result(AnimationPlaybackQueryResult.Status.SUBJECT_NOT_FOUND, animationId, subjectId,
                    "EANIM-A5-SUBJECT", "Animation subject not found.");
        }
        List<PlaybackRecord> active = data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .filter(playback -> playback.animationId.equals(animationId))
                .toList();
        if (active.size() > 1) {
            return result(AnimationPlaybackQueryResult.Status.AMBIGUOUS_PLAYBACK, animationId, subjectId,
                    "EANIM-A5-AMBIGUOUS", "More than one active animation matches.");
        }
        if (active.size() == 1) {
            return active(active.get(0), subject.revision);
        }
        Optional<TerminalRecord> terminal = data.terminalPlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .filter(playback -> playback.animationId.equals(animationId))
                .max(Comparator.comparingLong(playback -> playback.completionTick));
        if (terminal.isPresent()) {
            return terminal(terminal.orElseThrow(), subject.revision);
        }
        boolean knownDefinition = definitions.stream().anyMatch(definition -> definition.id().equals(animationId));
        return knownDefinition
                ? result(AnimationPlaybackQueryResult.Status.PLAYBACK_NOT_FOUND, animationId, subjectId,
                        "EANIM-A5-NO-PLAYBACK", "No matching active or terminal animation.")
                : result(AnimationPlaybackQueryResult.Status.ANIMATION_NOT_FOUND, animationId, subjectId,
                        "EANIM-A5-NOT-FOUND", "Animation definition not found.");
    }

    private AnimationPlaybackQueryResult active(PlaybackRecord playback, long subjectRevision) {
        return new AnimationPlaybackQueryResult(AnimationPlaybackQueryResult.Status.ACTIVE, playback.animationId,
                playback.subjectId, Optional.of(playback.playbackId), Optional.of(playback.phase), Optional.empty(),
                subjectRevision, playback.playbackRevision, "EANIM-A5-ACTIVE", "Animation playback is active.");
    }

    private AnimationPlaybackQueryResult terminal(TerminalRecord playback, long subjectRevision) {
        return new AnimationPlaybackQueryResult(AnimationPlaybackQueryResult.Status.TERMINAL, playback.animationId,
                playback.subjectId, Optional.of(playback.playbackId), Optional.empty(), Optional.of(playback.outcome),
                subjectRevision, playback.finalRevision, "EANIM-A5-TERMINAL", "Animation playback is terminal.");
    }

    private AnimationPlaybackQueryResult result(
            AnimationPlaybackQueryResult.Status status,
            AnimationId animationId,
            AnimationSubjectId subjectId,
            String code,
            String message
    ) {
        return new AnimationPlaybackQueryResult(status, animationId, subjectId, Optional.empty(), Optional.empty(),
                Optional.empty(), 0L, 0L, code, message);
    }
}
