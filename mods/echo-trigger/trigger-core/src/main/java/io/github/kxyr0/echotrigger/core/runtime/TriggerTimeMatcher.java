package io.github.kxyr0.echotrigger.core.runtime;

import java.util.Map;
import java.util.Optional;

final class TriggerTimeMatcher {
    private TriggerTimeMatcher() {
    }

    static boolean between(Map<String, String> arguments, Map<String, String> payload) {
        try {
            int start = Integer.parseInt(arguments.getOrDefault("start", "0"));
            int end = Integer.parseInt(arguments.getOrDefault("end", "0"));
            int value = Integer.parseInt(payload.getOrDefault("value", "-1"));
            return start <= end ? value >= start && value <= end : value >= start || value <= end;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    static boolean reached(double threshold, double current, Map<String, String> payload) {
        Optional<String> previous = Optional.ofNullable(payload.get("previousValue"));
        if (previous.isEmpty()) {
            return false;
        }
        try {
            double before = Double.parseDouble(previous.orElseThrow());
            return before <= current ? threshold > before && threshold <= current
                    : threshold > before || threshold <= current;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
