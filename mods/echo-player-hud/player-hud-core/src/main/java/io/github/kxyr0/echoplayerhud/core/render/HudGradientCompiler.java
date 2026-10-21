package io.github.kxyr0.echoplayerhud.core.render;

import io.github.kxyr0.echoplayerhud.api.gradient.HudColorStep;
import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;

/**
 * Resolves a stepped gradient color without rebuilding per frame.
 */
public final class HudGradientCompiler {
    public int colorAt(HudGradient gradient, double normalized) {
        double value = Math.max(0.0D, Math.min(1.0D, normalized));
        HudColorStep previous = gradient.steps().get(0);
        for (HudColorStep step : gradient.steps()) {
            if (step.at() >= value) {
                return colorBetween(previous, step, value);
            }
            previous = step;
        }
        return previous.argb();
    }

    private int colorBetween(HudColorStep start, HudColorStep end, double value) {
        if (start.at() == end.at()) {
            return end.argb();
        }
        double local = (value - start.at()) / (end.at() - start.at());
        return channel(start.argb(), end.argb(), local, 24)
                | channel(start.argb(), end.argb(), local, 16)
                | channel(start.argb(), end.argb(), local, 8)
                | channel(start.argb(), end.argb(), local, 0);
    }

    private int channel(int start, int end, double local, int shift) {
        int from = (start >>> shift) & 0xFF;
        int to = (end >>> shift) & 0xFF;
        return (int) Math.round(from + (to - from) * local) << shift;
    }
}
