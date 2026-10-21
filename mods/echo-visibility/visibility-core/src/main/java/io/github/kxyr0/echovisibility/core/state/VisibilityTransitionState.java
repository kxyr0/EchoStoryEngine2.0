package io.github.kxyr0.echovisibility.core.state;

import io.github.kxyr0.echovisibility.api.policy.VisibilityHysteresis;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import java.util.Objects;

public final class VisibilityTransitionState {
    private final VisibilityHysteresis hysteresis;
    private final boolean requireVisibleFirst;
    private VisibilityState stableState = VisibilityState.UNKNOWN;
    private long stableFrame = -1L;
    private int hiddenFrames;
    private int visibleFrames;
    private boolean everVisible;

    public VisibilityTransitionState(VisibilityHysteresis hysteresis, boolean requireVisibleFirst) {
        this.hysteresis = Objects.requireNonNull(hysteresis, "hysteresis");
        this.requireVisibleFirst = requireVisibleFirst;
    }

    public VisibilityTransitionResult observe(VisibilityState observed, long renderedFrame) {
        Objects.requireNonNull(observed, "observed");
        if (renderedFrame < 0L) {
            throw new IllegalArgumentException("Rendered frame must not be negative.");
        }
        boolean wasVisible = stableState.visible();
        boolean hidden = observed.geometricallyHidden();
        updateCounters(observed, hidden);
        VisibilityState previous = stableState;
        VisibilityTransitionKind kind = applyObservation(observed, wasVisible, hidden);
        boolean changed = previous != stableState;
        long previousStableFrame = stableFrame;
        if (changed) {
            stableFrame = renderedFrame;
        }
        VisibilityTransition transition = new VisibilityTransition(kind, previous, stableState,
                previousStableFrame, renderedFrame);
        return new VisibilityTransitionResult(observed, stableState, transition, status(),
                hiddenFrames, visibleFrames, changed || kind != VisibilityTransitionKind.NONE);
    }

    public VisibilityState stableState() {
        return stableState;
    }

    public boolean everVisible() {
        return everVisible;
    }

    private VisibilityTransitionKind applyObservation(
            VisibilityState observed,
            boolean wasVisible,
            boolean hidden) {
        if (observed.visible() && visibleFrames >= hysteresis.seenFrames()) {
            return applyVisible(observed, wasVisible);
        }
        if (readyHidden(hidden)) {
            stableState = observed;
            return wasVisible ? VisibilityTransitionKind.LOST_SIGHT : VisibilityTransitionKind.NONE;
        }
        return applyUnavailable(observed);
    }

    private VisibilityTransitionKind applyVisible(VisibilityState observed, boolean wasVisible) {
        stableState = observed;
        if (!everVisible) {
            everVisible = true;
            return VisibilityTransitionKind.FIRST_VISIBLE;
        }
        return wasVisible ? VisibilityTransitionKind.NONE : VisibilityTransitionKind.GAINED_SIGHT;
    }

    private boolean readyHidden(boolean hidden) {
        return hidden && hiddenFrames >= hysteresis.lostFrames()
                && (!requireVisibleFirst || everVisible);
    }

    private VisibilityTransitionKind applyUnavailable(VisibilityState observed) {
        if (observed == VisibilityState.TARGET_UNAVAILABLE) {
            stableState = observed;
            return VisibilityTransitionKind.TARGET_BECAME_UNAVAILABLE;
        }
        if (observed == VisibilityState.CAMERA_UNAVAILABLE) {
            stableState = observed;
            return VisibilityTransitionKind.CAMERA_BECAME_UNAVAILABLE;
        }
        return VisibilityTransitionKind.NONE;
    }

    private void updateCounters(VisibilityState observed, boolean hidden) {
        if (observed.visible()) {
            visibleFrames++;
            hiddenFrames = 0;
        } else if (hidden) {
            hiddenFrames++;
            visibleFrames = 0;
        } else {
            visibleFrames = 0;
            hiddenFrames = 0;
        }
    }

    private VisibilityWatchStatus status() {
        if (stableState == VisibilityState.TARGET_UNAVAILABLE) {
            return VisibilityWatchStatus.TARGET_UNAVAILABLE;
        }
        if (!everVisible && requireVisibleFirst) {
            return VisibilityWatchStatus.ARMED_WAITING_FOR_VISIBLE;
        }
        if (stableState.visible()) {
            return VisibilityWatchStatus.TRACKING_VISIBLE;
        }
        return stableState.geometricallyHidden()
                ? VisibilityWatchStatus.LOST_SIGHT
                : VisibilityWatchStatus.ARMED_WAITING_FOR_VISIBLE;
    }
}
