package io.github.kxyr0.echothoughtspace.core.lifecycle;

public final class ThoughtSpaceTransition {
    private final long durationNanos;
    private ThoughtSpaceState state = ThoughtSpaceState.CLOSED;
    private long phaseStartedNanos;

    public ThoughtSpaceTransition(long durationMillis) {
        if (durationMillis < 1L) throw new IllegalArgumentException("Duration must be positive.");
        durationNanos = durationMillis * 1_000_000L;
    }

    public void open(long nowNanos) {
        if (state != ThoughtSpaceState.CLOSED) return;
        state = ThoughtSpaceState.OPENING;
        phaseStartedNanos = nowNanos;
    }

    public void close(long nowNanos) {
        if (state != ThoughtSpaceState.OPEN) return;
        state = ThoughtSpaceState.CLOSING;
        phaseStartedNanos = nowNanos;
    }

    public void update(long nowNanos) {
        if (state == ThoughtSpaceState.OPENING && progress(nowNanos) >= 1.0D) {
            state = ThoughtSpaceState.OPEN;
        }
    }

    public void finishClosed() {
        if (state != ThoughtSpaceState.CLOSING) throw new IllegalStateException("Not closing.");
        state = ThoughtSpaceState.CLOSED;
    }

    public void resetClosed() { state = ThoughtSpaceState.CLOSED; }

    public ThoughtSpaceState state() { return state; }
    public boolean captureActive() { return state != ThoughtSpaceState.CLOSED; }
    public boolean closeFrameComplete(long nowNanos) {
        return state == ThoughtSpaceState.CLOSING && progress(nowNanos) >= 1.0D;
    }
    public double opacity(long nowNanos) {
        if (state == ThoughtSpaceState.CLOSED) return 0.0D;
        if (state == ThoughtSpaceState.OPEN) return 1.0D;
        double value = ease(progress(nowNanos));
        return state == ThoughtSpaceState.OPENING ? value : 1.0D - value;
    }

    private double progress(long nowNanos) {
        return Math.max(0.0D, Math.min(1.0D, (nowNanos - phaseStartedNanos) / (double) durationNanos));
    }
    private static double ease(double value) { return value * value * (3.0D - 2.0D * value); }
}
