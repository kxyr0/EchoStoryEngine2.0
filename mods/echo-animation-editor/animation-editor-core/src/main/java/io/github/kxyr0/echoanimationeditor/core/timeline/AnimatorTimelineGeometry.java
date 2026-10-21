package io.github.kxyr0.echoanimationeditor.core.timeline;

public record AnimatorTimelineGeometry(int durationFrames, int left, int right) {
    public AnimatorTimelineGeometry {
        if (durationFrames <= 0) {
            throw new IllegalArgumentException("durationFrames must be positive");
        }
        if (right <= left) {
            throw new IllegalArgumentException("timeline bounds are invalid");
        }
    }

    public int frameToX(int frame) {
        int clamped = Math.max(0, Math.min(durationFrames, frame));
        double progress = (double) clamped / durationFrames;
        return left + (int) Math.round((right - left) * progress);
    }

    public int xToFrame(int x) {
        int clamped = Math.max(left, Math.min(right, x));
        double progress = (double) (clamped - left) / (right - left);
        return (int) Math.round(durationFrames * progress);
    }
}
