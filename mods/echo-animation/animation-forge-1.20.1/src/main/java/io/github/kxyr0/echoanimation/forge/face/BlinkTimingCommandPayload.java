package io.github.kxyr0.echoanimation.forge.face;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record BlinkTimingCommandPayload(String subjectId, BlinkTimingOverride timing) {
    BlinkTimingCommandPayload {
        subjectId = require(subjectId, "subjectId");
        timing = Objects.requireNonNull(timing, "timing");
    }

    static BlinkTimingCommandPayload decode(String encoded) {
        Map<String, String> values = query(encoded);
        String subject = first(values, "subject", "npc", "npcId", "actor")
                .orElseThrow(() -> new IllegalArgumentException("Missing subject."));
        return new BlinkTimingCommandPayload(subject, new BlinkTimingOverride(
                integer(values, "periodTicks", "period"),
                integer(values, "closeTicks", "close"),
                integer(values, "holdTicks", "hold"),
                integer(values, "openTicks", "open")));
    }

    static String decodeSubject(String encoded) {
        return first(query(encoded), "subject", "npc", "npcId", "actor")
                .orElseThrow(() -> new IllegalArgumentException("Missing subject."));
    }

    private static int integer(Map<String, String> values, String... names) {
        String value = first(values, names)
                .orElseThrow(() -> new IllegalArgumentException("Missing " + names[0] + "."));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(names[0] + " must be an integer.");
        }
    }

    private static Map<String, String> query(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Map.of();
        }
        Map<String, String> output = new LinkedHashMap<>();
        for (String part : encoded.split("&", -1)) {
            if (!part.isEmpty()) {
                readPart(output, part);
            }
        }
        return Map.copyOf(output);
    }

    private static void readPart(Map<String, String> output, String part) {
        int separator = part.indexOf('=');
        String key = separator >= 0 ? part.substring(0, separator) : part;
        String value = separator >= 0 ? part.substring(separator + 1) : "";
        output.put(decodePart(key), decodePart(value));
    }

    private static Optional<String> first(Map<String, String> values, String... names) {
        for (String name : names) {
            String value = values.get(name);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String require(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
