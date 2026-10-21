package io.github.kxyr0.echoanimation.forge.debug;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local face debug switches for integrated testing.
 */
public final class FaceDebugState {
    private static final AtomicReference<Mode> MODE = new AtomicReference<>(Mode.OFF);

    private FaceDebugState() {
    }

    public static Mode mode() {
        return MODE.get();
    }

    public static void set(Mode mode) {
        MODE.set(java.util.Objects.requireNonNull(mode, "mode"));
    }

    public static boolean enabled() {
        return mode() != Mode.OFF;
    }

    public static boolean origins() {
        Mode mode = mode();
        return mode == Mode.ORIGINS || mode == Mode.ALL;
    }

    public static boolean pivots() {
        Mode mode = mode();
        return mode == Mode.PIVOTS || mode == Mode.ALL;
    }

    public enum Mode {
        OFF, ORIGINS, PIVOTS, ALL
    }
}
