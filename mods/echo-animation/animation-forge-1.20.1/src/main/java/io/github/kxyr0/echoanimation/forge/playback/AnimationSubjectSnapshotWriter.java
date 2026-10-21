package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationPlaybackSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.snapshot.AnimationSnapshotChannelPolicy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds authoritative sync snapshots from durable A3 playback records.
 */
public final class AnimationSubjectSnapshotWriter {
    private final AnimationSnapshotChannelPolicy channelPolicy = new AnimationSnapshotChannelPolicy();
    public AnimationSubjectSyncSnapshot snapshot(SnapshotInput input) {
        List<AnimationChannelSyncState> channels = input.subject().channelStates.keySet().stream()
                .sorted().map(channel -> channel(input, channel)).toList();
        List<AnimationPlaybackSyncState> playbacks = input.active().stream()
                .map(playback -> playback(input, playback)).toList();
        return new AnimationSubjectSyncSnapshot(input.subject().subjectId, input.graphRevision(), input.tick(),
                new AnimationSyncRevision(input.subject().revision), channels, playbacks);
    }

    private AnimationChannelSyncState channel(SnapshotInput input, AnimationChannelId channelId) {
        Optional<PlaybackRecord> active = activeOn(input, channelId);
        if (active.isEmpty()) {
            return idle(input.subject().channelStates.get(channelId), channelId);
        }
        PlaybackRecord playback = active.orElseThrow();
        AnimationDefinition definition = input.definitions().get(playback.definitionFingerprint);
        Optional<AnimationClipId> clipId = phaseClip(playback);
        long duration = duration(definition, playback);
        double visualElapsed = visualElapsed(playback);
        PlaybackMode mode = playbackMode(playback, definition);
        return new AnimationChannelSyncState(channelId, phaseState(playback), clipId, phaseSegment(playback),
                phasePoint(playback),
                localTick(playback, duration, mode), Math.max(0L, (long) Math.floor(visualElapsed)),
                duration, progress(playback, visualElapsed, duration, mode), playback.mainLoopIteration,
                complete(playback, duration), waiting(playback, definition), mode,
                playback.playbackSpeed, 0, ChannelCompositionPolicy.OVERRIDE,
                channelPolicy.boneMask(definition, input.clips(), clipId),
                channelPolicy.weight(playback, definition, duration),
                Optional.empty(), 0L, 0L, 0.0D);
    }

    private AnimationChannelSyncState idle(AnimationStateId stateId, AnimationChannelId channelId) {
        return new AnimationChannelSyncState(channelId, stateId, Optional.empty(), Optional.empty(), Optional.empty(),
                0L, 0L, 1L, 0.0D, 0L, true,
                false, PlaybackMode.STOP_AT_END, 1.0D, 0, ChannelCompositionPolicy.OVERRIDE, Set.of(), 1.0D,
                Optional.empty(), 0L, 0L, 0.0D);
    }

    private Optional<PlaybackRecord> activeOn(SnapshotInput input, AnimationChannelId channelId) {
        return input.active().stream().filter(playback -> playback.leases.containsKey(channelId)).findFirst();
    }

    private AnimationPlaybackSyncState playback(SnapshotInput input, PlaybackRecord playback) {
        return new AnimationPlaybackSyncState(playback.playbackId, playback.animationId,
                playback.definitionFingerprint, playback.phase, playback.phaseEnteredTick,
                playback.phaseElapsedTicks, playback.mainLoopIteration, playback.exitRequested,
                playback.selectedExitPoint, playback.targetState, playback.playbackRevision);
    }

    private Optional<AnimationClipId> phaseClip(PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> Optional.of(playback.entryClipId);
            case MAIN -> Optional.of(playback.mainClipId);
            case EXIT -> playback.exitClipId;
            case TERMINAL -> Optional.empty();
        };
    }

    private Optional<String> phaseSegment(PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> playback.entrySegmentId;
            case MAIN -> playback.mainSegmentId;
            case EXIT -> playback.exitSegmentId;
            case TERMINAL -> Optional.empty();
        };
    }

    private Optional<String> phasePoint(PlaybackRecord playback) {
        return playback.phase == AnimationPhase.MAIN || playback.fallbackExit
                ? playback.mainPointId : Optional.empty();
    }

    private AnimationStateId phaseState(PlaybackRecord playback) {
        return new AnimationStateId("playback/" + playback.playbackId.value() + "/" + playback.phase.name()
                .toLowerCase(java.util.Locale.ROOT));
    }

    private long duration(AnimationDefinition definition, PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> Math.max(1L, definition.entryVariants().stream()
                    .filter(entry -> entry.clipId().equals(playback.entryClipId))
                    .filter(entry -> entry.segmentId().equals(playback.entrySegmentId)).findFirst().orElseThrow()
                    .durationTicks());
            case MAIN -> definition.main().durationTicks();
            case EXIT -> playback.fallbackExit ? Math.max(1L, definition.blendPolicy().exitTicks())
                    : Math.max(1L, definition.exitVariants().stream()
                            .filter(exit -> playback.selectedExitPoint.filter(exit.exitPointId()::equals).isPresent())
                            .findFirst().orElseThrow().durationTicks());
            case TERMINAL -> 1L;
        };
    }

    private long localTick(PlaybackRecord playback, long duration, PlaybackMode mode) {
        double visualElapsed = visualElapsed(playback);
        if (mode == PlaybackMode.LOOP && duration > 0L) {
            return Math.max(0L, (long) Math.floor(visualElapsed % duration));
        }
        return Math.min(Math.max(0L, (long) Math.floor(visualElapsed)), duration);
    }

    private double progress(PlaybackRecord playback, double visualElapsed, long duration, PlaybackMode mode) {
        if (playback.fallbackExit) {
            return 1.0D;
        }
        if (duration <= 0L) {
            return 1.0D;
        }
        if (mode != PlaybackMode.LOOP) {
            return Math.max(0.0D, Math.min(1.0D, visualElapsed / (double) duration));
        }
        double local = visualElapsed % duration;
        return Math.max(0.0D, Math.min(1.0D, local / (double) duration));
    }

    private double visualElapsed(PlaybackRecord playback) {
        if (Double.isFinite(playback.sampleElapsedTicks) && playback.sampleElapsedTicks >= 0.0D) {
            return playback.sampleElapsedTicks;
        }
        return playback.phaseElapsedTicks * playback.playbackSpeed;
    }

    private boolean complete(PlaybackRecord playback, long duration) {
        return playback.phase != AnimationPhase.MAIN && playback.phaseElapsedTicks >= duration;
    }

    private boolean waiting(PlaybackRecord playback, AnimationDefinition definition) {
        return playback.phase == AnimationPhase.MAIN && playback.exitRequested
                && definition.main().mode() == MainMode.LOOP_UNTIL_STOP;
    }

    private PlaybackMode playbackMode(PlaybackRecord playback, AnimationDefinition definition) {
        if (playback.phase != AnimationPhase.MAIN) {
            return PlaybackMode.STOP_AT_END;
        }
        return switch (definition.main().mode()) {
            case LOOP_UNTIL_STOP -> PlaybackMode.LOOP;
            case HOLD_UNTIL_STOP -> PlaybackMode.HOLD_LAST_FRAME;
            case ONCE -> PlaybackMode.STOP_AT_END;
        };
    }

    public record SnapshotInput(
            SubjectRecord subject,
            List<PlaybackRecord> active,
            Map<String, AnimationDefinition> definitions,
            Map<AnimationClipId, AnimationClipDefinition> clips,
            long graphRevision,
            long tick
    ) {
    }
}
