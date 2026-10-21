package io.github.kxyr0.echoanimation.core.runtime;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.core.playback.PlaybackCursor;
import java.util.Optional;

final class AnimationChannelSlot {
    private final AnimationChannelId channelId;
    private AnimationStateDefinition state;
    private AnimationStateDefinition previousState;
    private long elapsedTick;
    private long transitionTick;
    private long transitionDurationTicks;
    private boolean waitingForExit;

    AnimationChannelSlot(AnimationChannelId channelId, AnimationStateDefinition state) {
        this.channelId = channelId;
        this.state = state;
    }

    AnimationChannelId channelId() {
        return channelId;
    }

    AnimationStateDefinition state() {
        return state;
    }

    void enter(AnimationStateDefinition nextState) {
        enter(nextState, 0L);
    }

    void enter(AnimationStateDefinition nextState, long blendDurationTicks) {
        previousState = blendDurationTicks > 0L ? state : null;
        state = nextState;
        elapsedTick = 0L;
        transitionTick = 0L;
        transitionDurationTicks = blendDurationTicks;
        waitingForExit = false;
    }

    void tick(long logicalTicks) {
        if (logicalTicks < 0L) {
            throw new IllegalArgumentException("logicalTicks must not be negative.");
        }
        if (logicalTicks == 0L) {
            return;
        }
        elapsedTick += logicalTicks;
        if (transitionDurationTicks > 0L) {
            transitionTick = Math.min(transitionDurationTicks, transitionTick + logicalTicks);
            if (transitionTick >= transitionDurationTicks) {
                previousState = null;
            }
        }
        waitingForExit = PlaybackCursor.from(state, elapsedTick).waitingForExit();
    }

    AnimationSubjectSnapshot.ChannelView view() {
        PlaybackCursor cursor = PlaybackCursor.from(state, elapsedTick);
        return new AnimationSubjectSnapshot.ChannelView(
                state.id(),
                state.clipId(),
                cursor.sampledTick(),
                cursor.elapsedTicks(),
                cursor.normalizedProgress(),
                cursor.loopIteration(),
                cursor.clipComplete(),
                waitingForExit,
                transition());
    }

    private Optional<AnimationSubjectSnapshot.TransitionView> transition() {
        if (previousState == null || transitionDurationTicks <= 0L || transitionTick >= transitionDurationTicks) {
            return Optional.empty();
        }
        double progress = transitionTick / (double) transitionDurationTicks;
        return Optional.of(new AnimationSubjectSnapshot.TransitionView(
                previousState.id(), state.id(), transitionTick, transitionDurationTicks, progress));
    }
}
