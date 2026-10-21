package io.github.kxyr0.echoanimation.forge.playback.state;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Server-side exclusive posture lock for the authoritative BODY channel. */
public final class BodyPostureLockController {
    private static final AnimationChannelId BODY = new AnimationChannelId("body");
    private final Set<AnimationSubjectId> locked = new HashSet<>();

    public boolean setLocked(AnimationRuntimeSavedData data, AnimationSubjectId subjectId, boolean value) {
        java.util.Objects.requireNonNull(subjectId, "subjectId");
        if (!value) {
            locked.remove(subjectId);
            return false;
        }
        locked.add(subjectId);
        return cancelBodyPlayback(data, subjectId);
    }

    public boolean locked(AnimationSubjectId subjectId) {
        return locked.contains(java.util.Objects.requireNonNull(subjectId, "subjectId"));
    }

    public boolean rejects(AnimationSubjectId subjectId, Optional<AnimationDefinition> definition) {
        return locked(subjectId)
                && definition.map(item -> item.controlledChannels().contains(BODY)).orElse(false);
    }

    public void remove(AnimationSubjectId subjectId) {
        locked.remove(subjectId);
    }

    public void clear() {
        locked.clear();
    }

    private boolean cancelBodyPlayback(AnimationRuntimeSavedData data, AnimationSubjectId subjectId) {
        SubjectRecord subject = data.subjects.get(subjectId);
        if (subject == null) {
            return false;
        }
        var matches = data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .filter(playback -> playback.leases.containsKey(BODY))
                .toList();
        if (matches.isEmpty()) {
            return false;
        }
        matches.forEach(playback -> {
            data.activePlaybacks.remove(playback.playbackId);
            playback.leases.keySet().forEach(channel -> subject.channelStates.put(channel, playback.targetState));
        });
        subject.revision++;
        data.setDirtyRevision();
        return true;
    }
}
