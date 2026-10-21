package io.github.kxyr0.echocutscene.forge.client.authoring;

public final class ClientFrameClock {
    private static final double MAX_FRAME_SECONDS = 0.1D;
    private long lastFrameAtMillis;

    public void reset(long nowMillis) {
        lastFrameAtMillis = nowMillis;
    }

    public double seconds(long nowMillis) {
        if (lastFrameAtMillis <= 0L) {
            lastFrameAtMillis = nowMillis;
            return 0.0D;
        }
        long elapsedMillis = Math.max(0L, nowMillis - lastFrameAtMillis);
        lastFrameAtMillis = nowMillis;
        return Math.min(MAX_FRAME_SECONDS, elapsedMillis / 1000.0D);
    }
}
