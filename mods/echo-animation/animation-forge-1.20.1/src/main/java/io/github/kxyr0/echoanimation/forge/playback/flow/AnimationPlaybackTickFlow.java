package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.TerminalRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import io.github.kxyr0.echoanimation.forge.playback.flow.timing.AnimationPhaseTiming;
import io.github.kxyr0.echoanimation.forge.playback.marker.AnimationMarkerDeliveryFlow;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Advances active playback cursors on the server logical tick. */
public final class AnimationPlaybackTickFlow {
    private final AnimationMarkerDeliveryFlow markerDelivery;
    private final AnimationPhaseTiming timing = new AnimationPhaseTiming();

    public AnimationPlaybackTickFlow() {
        this(event -> {});
    }

    public AnimationPlaybackTickFlow(Consumer<AnimationMarkerEvent> markerConsumer) {
        markerDelivery = new AnimationMarkerDeliveryFlow(markerConsumer);
    }

    public Set<AnimationSubjectId> tick(AnimationRuntimeSavedData data, long serverTick) {
        return List.copyOf(data.activePlaybacks.values()).stream() .map(playback -> advance(data, playback, serverTick))
                .flatMap(Optional::stream) .collect(Collectors.toSet());
    }

    private Optional<AnimationSubjectId> advance(
            AnimationRuntimeSavedData data, PlaybackRecord playback, long serverTick) {
        AnimationDefinition definition = Optional.ofNullable(data.definitionArchive.get(playback.definitionFingerprint))
                        .map(ArchivedDefinition::definition) .orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        advanceClock(playback, definition);
        markerDelivery.deliver(data, playback, definition, serverTick);
        checkpoint(data, playback, definition);
        return switch (playback.phase) {
            case ENTER ->
                    playback.phaseElapsedTicks >= timing.duration(definition, playback) ? transition( data, playback,
                                    AnimationPhase.MAIN, Optional.empty(), serverTick) : Optional.empty();
            case MAIN -> main(data, playback, definition, serverTick);
            case EXIT ->
                    playback.phaseElapsedTicks >= timing.duration(definition, playback)
                            ? complete(data, playback, serverTick) : Optional.empty();
            case TERMINAL -> Optional.empty();
        };
    }

    private void advanceClock(PlaybackRecord playback, AnimationDefinition definition) {
        playback.phaseElapsedTicks++;
        if (!Double.isFinite(playback.sampleElapsedTicks) || playback.sampleElapsedTicks < 0.0D) {
            playback.sampleElapsedTicks = Math.max(0L, playback.phaseElapsedTicks - 1L) * playback.playbackSpeed;
        }
        playback.sampleElapsedTicks += playback.playbackSpeed;
        if (playback.phase == AnimationPhase.MAIN) {
            playback.mainLoopIteration = (long)
                            Math.floor( playback.sampleElapsedTicks
                                            / Math.max(1L, definition.main().durationTicks()));
        }
    }

    private Optional<AnimationSubjectId> main( AnimationRuntimeSavedData data, PlaybackRecord playback,
            AnimationDefinition definition, long serverTick) {
        if (definition.main().mode() == MainMode.ONCE && (playback.exitRequested
                        || playback.phaseElapsedTicks >= definition.main().durationTicks())) {
            return finishMain(data, playback, definition, serverTick);
        }
        if (definition.main().mode() == MainMode.HOLD_UNTIL_STOP && playback.exitRequested) {
            return finishMain(data, playback, definition, serverTick);
        }
        if (definition.main().mode() == MainMode.LOOP_UNTIL_STOP && playback.exitRequested
                && timing.atExitPoint(playback, definition)) {
            return finishMain(data, playback, definition, serverTick);
        }
        return Optional.empty();
    }

    private Optional<AnimationSubjectId> finishMain( AnimationRuntimeSavedData data, PlaybackRecord playback,
            AnimationDefinition definition, long serverTick) {
        if (definition.exitVariants().isEmpty()) {
            if (definition.blendPolicy().exitTicks() > 0) {
                playback.fallbackExit = true;
                playback.exitClipId = Optional.of(playback.mainClipId);
                playback.exitSegmentId = playback.mainSegmentId;
                return transition( data, playback, AnimationPhase.EXIT, Optional.empty(), serverTick);
            }
            return complete(data, playback, serverTick);
        }
        return transition( data, playback, AnimationPhase.EXIT, Optional.of(timing.firstExitPoint(definition)),
                serverTick);
    }

    private Optional<AnimationSubjectId> transition( AnimationRuntimeSavedData data, PlaybackRecord playback,
            AnimationPhase phase, Optional<String> exitPoint, long serverTick) {
        playback.phase = phase;
        playback.phaseElapsedTicks = 0L;
        playback.sampleElapsedTicks = 0.0D;
        playback.phaseEnteredTick = serverTick;
        playback.selectedExitPoint = exitPoint;
        exitPoint .flatMap(point -> exitVariant(data, playback, point)) .ifPresent( exit -> {
                            playback.exitClipId = Optional.of(exit.clipId());
                            playback.exitSegmentId = exit.segmentId();
                        });
        playback.playbackRevision++;
        SubjectRecord subject = data.subjects.get(playback.subjectId);
        playback.leases .keySet() .forEach(channel -> subject.channelStates.put(channel, phaseState(playback)));
        subject.revision++;
        data.setDirtyRevision();
        return Optional.of(playback.subjectId);
    }

    private Optional<AnimationSubjectId> complete(
            AnimationRuntimeSavedData data, PlaybackRecord playback, long serverTick) {
        SubjectRecord subject = data.subjects.get(playback.subjectId);
        playback.leases .keySet() .forEach(channel -> subject.channelStates.put(channel, playback.targetState));
        subject.revision++;
        data.activePlaybacks.remove(playback.playbackId);
        TerminalRecord completed = terminal(playback, serverTick);
        data.terminalPlaybacks.put(playback.playbackId, completed);
        data.setDirtyRevision();
        io.github.kxyr0.echoanimation.forge.domain.AnimationDomainEvents.completed( playback, completed);
        return Optional.of(playback.subjectId);
    }

    private TerminalRecord terminal(PlaybackRecord playback, long serverTick) {
        TerminalRecord terminal = new TerminalRecord();
        terminal.playbackId = playback.playbackId;
        terminal.animationId = playback.animationId;
        terminal.subjectId = playback.subjectId;
        terminal.outcome = playback.exitRequested ? AnimationPlaybackOutcome.GRACEFULLY_STOPPED
                        : AnimationPlaybackOutcome.COMPLETED;
        terminal.finalRevision = playback.playbackRevision;
        terminal.completionTick = serverTick;
        terminal.commandId = playback.commandId;
        return terminal;
    }

    private Optional<AnimationDefinition.ExitVariant> exitVariant(
            AnimationRuntimeSavedData data, PlaybackRecord playback, String point) {
        return Optional.ofNullable(data.definitionArchive.get(playback.definitionFingerprint))
                .map(ArchivedDefinition::definition) .flatMap( definition ->
                                definition.exitVariants().stream() .filter(exit -> exit.exitPointId().equals(point))
                                        .findFirst());
    }

    private AnimationStateId phaseState(PlaybackRecord playback) {
        return new AnimationStateId( "playback/" + playback.playbackId.value() + "/"
                        + playback.phase.name().toLowerCase(java.util.Locale.ROOT));
    }

    private void checkpoint( AnimationRuntimeSavedData data, PlaybackRecord playback, AnimationDefinition definition) {
        if (definition.recoveryPolicy().name().equals("EXACT") || definition.checkpointIntervalTicks() > 0
                        && playback.phaseElapsedTicks % definition.checkpointIntervalTicks()
                                == 0L) {
            data.setDirtyRevision();
        }
    }
}
