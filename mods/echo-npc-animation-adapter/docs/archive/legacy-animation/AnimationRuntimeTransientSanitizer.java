package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Handles session-derived animation state at both persistence boundaries:
 * startup sanitization for old saves and a persistence snapshot that excludes
 * transient locomotion/system state from new saves.
 */
public final class AnimationRuntimeTransientSanitizer {
    private AnimationRuntimeTransientSanitizer() {
    }

    /** Removes stale derived state from a loaded runtime before first sync. */
    public static void sanitize(AnimationRuntimeSavedData data) {
        Set<AnimationPlaybackId> removedPlaybackIds = new LinkedHashSet<>();
        boolean changed = false;

        for (PlaybackRecord playback : java.util.List.copyOf(data.activePlaybacks.values())) {
            if (!AnimationPlaybackPersistencePolicy.transientPlayback(playback)) {
                continue;
            }
            data.activePlaybacks.remove(playback.playbackId);
            removedPlaybackIds.add(playback.playbackId);
            restoreSubjectState(data.subjects.get(playback.subjectId), playback);
            changed = true;
        }

        for (var terminal : java.util.List.copyOf(data.terminalPlaybacks.values())) {
            if (!AnimationPlaybackPersistencePolicy.transientAnimation(terminal.animationId)) {
                continue;
            }
            data.terminalPlaybacks.remove(terminal.playbackId);
            removedPlaybackIds.add(terminal.playbackId);
            changed = true;
        }

        for (var command : java.util.List.copyOf(data.commandLedger.values())) {
            if (AnimationPlaybackPersistencePolicy.transientCommand(command)
                    || command.playbackId.map(removedPlaybackIds::contains).orElse(false)) {
                data.commandLedger.remove(command.commandId);
                changed = true;
            }
        }

        if (changed) {
            data.setDirtyRevision();
        }
    }

    /**
     * Builds an NBT-write view without touching the live runtime. WALK/RUN can
     * keep playing in memory while SavedData receives a clean durable snapshot.
     */
    public static AnimationRuntimeSavedData snapshotForPersistence(AnimationRuntimeSavedData live) {
        AnimationRuntimeSavedData snapshot = new AnimationRuntimeSavedData();
        snapshot.repositoryRevision = live.repositoryRevision;
        live.subjects.forEach((id, subject) -> snapshot.subjects.put(id, copySubject(subject)));
        snapshot.definitionArchive.putAll(live.definitionArchive);

        Set<AnimationPlaybackId> transientPlaybackIds = new LinkedHashSet<>();
        for (PlaybackRecord playback : live.activePlaybacks.values()) {
            if (AnimationPlaybackPersistencePolicy.transientPlayback(playback)) {
                transientPlaybackIds.add(playback.playbackId);
                restoreSubjectState(snapshot.subjects.get(playback.subjectId), playback);
            } else {
                snapshot.activePlaybacks.put(playback.playbackId, playback);
            }
        }
        for (var terminal : live.terminalPlaybacks.values()) {
            if (AnimationPlaybackPersistencePolicy.transientAnimation(terminal.animationId)) {
                transientPlaybackIds.add(terminal.playbackId);
            } else {
                snapshot.terminalPlaybacks.put(terminal.playbackId, terminal);
            }
        }
        for (var command : live.commandLedger.values()) {
            if (AnimationPlaybackPersistencePolicy.transientCommand(command)
                    || command.playbackId.map(transientPlaybackIds::contains).orElse(false)) {
                continue;
            }
            snapshot.commandLedger.put(command.commandId, command);
        }
        return snapshot;
    }

    private static SubjectRecord copySubject(SubjectRecord source) {
        SubjectRecord copy = new SubjectRecord(source.subjectId);
        copy.channelStates.putAll(source.channelStates);
        copy.provenance = source.provenance;
        copy.profileId = source.profileId;
        copy.profileFingerprint = source.profileFingerprint;
        copy.pendingProfileId = source.pendingProfileId;
        copy.pendingProfileFingerprint = source.pendingProfileFingerprint;
        copy.generation = source.generation;
        copy.revision = source.revision;
        return copy;
    }

    private static void restoreSubjectState(SubjectRecord subject, PlaybackRecord playback) {
        if (subject == null) {
            return;
        }
        boolean changed = false;
        for (var channel : playback.leases.keySet()) {
            subject.channelStates.put(channel, playback.targetState);
            changed = true;
        }
        if (changed) {
            subject.revision++;
        }
    }
}
