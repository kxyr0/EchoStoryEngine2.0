package io.github.kxyr0.echodialoguecutscene.forge.screen;

import java.util.Map;
import java.util.Optional;

final class ScreenPayload {
    private final double canvasGroup;
    private final long timeMillis;

    private ScreenPayload(double canvasGroup, long timeMillis) {
        this.canvasGroup = canvasGroup;
        this.timeMillis = Math.max(0L, timeMillis);
    }

    static ScreenPayload decode(Map<String, String> payload) {
        return new ScreenPayload(canvasGroup(payload), timeMillis(payload));
    }

    double canvasGroup() {
        return canvasGroup;
    }

    long timeMillis() {
        return timeMillis;
    }

    Map<String, String> outputs() {
        return Map.of("canvasGroup", Double.toString(canvasGroup), "time", Long.toString(timeMillis));
    }

    private static double canvasGroup(Map<String, String> payload) {
        return Double.parseDouble(requiredEither(payload, "canvasGroup", "alpha"));
    }

    private static long timeMillis(Map<String, String> payload) {
        double raw = Double.parseDouble(requiredEither(payload, "time", "durationMillis"));
        return Math.max(0L, Math.round(raw));
    }

    private static String requiredEither(Map<String, String> payload, String primary, String alias) {
        return Optional.ofNullable(payload.get(primary))
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(payload.get(alias)).filter(value -> !value.isBlank()))
                .orElseThrow(() -> new IllegalArgumentException(primary + " is required."));
    }
}
