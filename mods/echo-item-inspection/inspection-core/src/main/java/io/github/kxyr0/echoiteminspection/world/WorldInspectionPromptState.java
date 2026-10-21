package io.github.kxyr0.echoiteminspection.world;

/** Visibility grace never grants interaction with a target that is no longer hit. */
public final class WorldInspectionPromptState<T> {
    public static final long GRACE_MILLIS = 100;
    public static final long FADE_MILLIS = 140;
    private T displayTarget;
    private T activeTarget;
    private long lastSeen;
    private long transitionStarted;
    private double startAlpha;
    private double endAlpha;

    public void update(T target, long now) {
        activeTarget = target;
        if (target != null) {
            displayTarget = target;
            lastSeen = now;
            transition(1, now);
        } else if (now - lastSeen >= GRACE_MILLIS) transition(0, now);
    }

    private void transition(double destination, long now) {
        if (destination == endAlpha) return;
        startAlpha = alpha(now);
        endAlpha = destination;
        transitionStarted = now;
    }

    public double alpha(long now) {
        double progress = Math.max(0, Math.min(1, (now - transitionStarted) / (double) FADE_MILLIS));
        double eased = progress * progress * (3 - 2 * progress);
        return startAlpha + (endAlpha - startAlpha) * eased;
    }

    public T activeTarget() { return activeTarget; }
    public T displayTarget() { return displayTarget; }

    public void clear() {
        activeTarget = null;
        displayTarget = null;
        startAlpha = 0;
        endAlpha = 0;
        lastSeen = 0;
    }
}
