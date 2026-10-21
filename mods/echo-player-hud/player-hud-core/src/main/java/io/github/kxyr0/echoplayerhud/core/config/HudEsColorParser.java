package io.github.kxyr0.echoplayerhud.core.config;

import io.github.kxyr0.echoplayerhud.api.gradient.HudColorStep;
import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;
import java.util.ArrayList;
import java.util.List;

final class HudEsColorParser {
    HudGradient gradient(HudEsFields fields) {
        return fields.value("gradient")
                .map(this::gradient)
                .orElseGet(() -> fromColorFields(fields));
    }

    int color(String value) {
        String normalized = value.strip();
        if ("none".equalsIgnoreCase(normalized) || "transparent".equalsIgnoreCase(normalized)) {
            return 0;
        }
        if (normalized.startsWith("#")) {
            return cssHex(normalized.substring(1));
        }
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            return argbHex(normalized.substring(2));
        }
        return (int) Long.parseLong(normalized);
    }

    private HudGradient fromColorFields(HudEsFields fields) {
        var start = fields.value("gradientStart", "gradient_start", "startColor", "start_color");
        var end = fields.value("gradientEnd", "gradient_end", "endColor", "end_color");
        if (start.isPresent() || end.isPresent()) {
            int startColor = color(start.orElse("#FFFFFFFF"));
            int endColor = color(end.orElse(start.orElse("#FFFFFFFF")));
            return new HudGradient(List.of(new HudColorStep(0.0D, startColor), new HudColorStep(1.0D, endColor)));
        }
        return fields.value("color")
                .map(value -> HudGradient.solid(color(value)))
                .orElseGet(() -> new HudGradient(List.of(
                        new HudColorStep(0.0D, color(fields.valueOr("#FFFFFFFF", "lowColor", "low_color"))),
                        new HudColorStep(0.66D, color(fields.valueOr("#FFFFFFFF", "highColor", "high_color")))
                )));
    }

    private HudGradient gradient(String value) {
        String[] parts = value.split("[;,]");
        List<HudColorStep> steps = new ArrayList<>();
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index].strip();
            if (part.isEmpty()) {
                continue;
            }
            int separator = separator(part);
            if (separator >= 0) {
                steps.add(new HudColorStep(Double.parseDouble(part.substring(0, separator).strip()),
                        color(part.substring(separator + 1).strip())));
            } else {
                double at = parts.length == 1 ? 0.0D : (double) index / (double) (parts.length - 1);
                steps.add(new HudColorStep(at, color(part)));
            }
        }
        return new HudGradient(steps);
    }

    private int separator(String part) {
        int colon = part.indexOf(':');
        int equals = part.indexOf('=');
        if (colon < 0) {
            return equals;
        }
        if (equals < 0) {
            return colon;
        }
        return Math.min(colon, equals);
    }

    private int cssHex(String value) {
        String normalized = value.strip();
        long parsed = Long.parseLong(normalized, 16);
        if (normalized.length() == 6) {
            return (int) (parsed | 0xFF000000L);
        }
        if (normalized.length() == 8) {
            long rgb = parsed >>> 8;
            long alpha = parsed & 0xFFL;
            return (int) ((alpha << 24) | rgb);
        }
        throw new IllegalArgumentException("CSS hex color must be #RRGGBB or #RRGGBBAA.");
    }

    private int argbHex(String value) {
        String normalized = value.strip();
        long parsed = Long.parseLong(normalized, 16);
        if (normalized.length() == 6) {
            parsed |= 0xFF000000L;
        }
        if (normalized.length() != 6 && normalized.length() != 8) {
            throw new IllegalArgumentException("ARGB hex color must be 0xRRGGBB or 0xAARRGGBB.");
        }
        return (int) parsed;
    }
}
