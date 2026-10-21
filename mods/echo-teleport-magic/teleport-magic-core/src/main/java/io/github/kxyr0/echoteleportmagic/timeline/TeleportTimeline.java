package io.github.kxyr0.echoteleportmagic.timeline;

/** Reference visual clock plus a longer runtime buildup; all departures share this mapping. */
public final class TeleportTimeline {
    public static final String PROFILE = "echo:golden_reference";
    public static final int REVISION = 2;
    public static final int VISUAL_COMMIT_TICK = 39;
    public static final int COMMIT_TICK = 47;
    public static final int COMPLETE_TICK = 88;
    public static final double COMMIT_SECONDS = COMMIT_TICK / 20.0;
    public static final double VISUAL_COMMIT_SECONDS = VISUAL_COMMIT_TICK / 20.0;
    private TeleportTimeline() { }

    public enum Phase { PRECHARGE, BUILDUP, SURGE, CONCEALMENT, AFTERBURST, RESIDUAL, COMPLETE }

    public static double visualTime(double elapsed) {
        return elapsed < COMMIT_SECONDS ? elapsed * VISUAL_COMMIT_SECONDS / COMMIT_SECONDS
                : elapsed - (COMMIT_SECONDS - VISUAL_COMMIT_SECONDS);
    }

    public static Phase phase(double t) {
        if (t < .30) return Phase.PRECHARGE;
        if (t < 1.20) return Phase.BUILDUP;
        if (t < 1.75) return Phase.SURGE;
        if (t < 2.15) return Phase.CONCEALMENT;
        if (t < 2.80) return Phase.AFTERBURST;
        return t < 4 ? Phase.RESIDUAL : Phase.COMPLETE;
    }
    public static float smooth(double low, double high, double t) {
        double x = Math.max(0, Math.min(1, (t - low) / (high - low)));
        return (float) (x * x * (3 - 2 * x));
    }
    public static float body(double t) {
        return (.15f * smooth(.3, 1.1, t) + .85f * smooth(1.1, 1.87, t)) * (1 - smooth(1.95, 2.1, t));
    }
    public static float flash(double t) { return smooth(1.7, 1.9, t) * (1 - smooth(2.12, 2.80, t)); }
    public static float ground(double t) { return smooth(.65, 1.95, t) * (1 - smooth(2.4, 4, t)); }
    public static float emission(double t) {
        if (t < 0 || t >= 3.3) return 0;
        return (.025f + .975f * smooth(0, 1.85, t)) * (1 - smooth(2.10, 3.30, t));
    }
    public static float arrival(double t) { return (1 - smooth(.15, 1.55, t)); }
}
