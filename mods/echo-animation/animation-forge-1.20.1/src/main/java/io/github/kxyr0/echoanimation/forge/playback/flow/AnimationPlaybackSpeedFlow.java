package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.Optional;

/**
 * Updates runtime playback speed without restarting the active animation.
 */
public final class AnimationPlaybackSpeedFlow {
    public Optional<AnimationSubjectId> update(
            AnimationRuntimeSavedData data,
            AnimationSubjectId subjectId,
            AnimationId animationId,
            double playbackSpeed
    ) {
        double normalized = normalized(playbackSpeed);
        Optional<PlaybackRecord> playback = data.activePlaybacks.values().stream()
                .filter(record -> record.subjectId.equals(subjectId))
                .filter(record -> record.animationId.equals(animationId))
                .findFirst();
        if (playback.isEmpty() || Math.abs(playback.orElseThrow().playbackSpeed - normalized) < 0.01D) {
            return Optional.empty();
        }
        SubjectRecord subject = data.subjects.get(subjectId);
        if (subject == null) {
            return Optional.empty();
        }
        PlaybackRecord record = playback.orElseThrow();
        record.playbackSpeed = normalized;
        record.playbackRevision++;
        subject.revision++;
        data.setDirtyRevision();
        return Optional.of(subjectId);
    }

    public double normalized(double playbackSpeed) {
        return Double.isFinite(playbackSpeed) && playbackSpeed > 0.0D ? playbackSpeed : 1.0D;
    }
}
