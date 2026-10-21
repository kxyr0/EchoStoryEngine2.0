package io.github.kxyr0.echodialogue.api.command;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stable string payload format for external dialogue commands.
 */
public final class DialogueCommandPayload {
    private DialogueCommandPayload() {
    }

    public static String encode(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static Map<String, String> decode(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String part : payload.split("&", -1)) {
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            String key = separator >= 0 ? part.substring(0, separator) : part;
            String value = separator >= 0 ? part.substring(separator + 1) : "";
            values.put(decodePart(key), decodePart(value));
        }
        return Collections.unmodifiableMap(values);
    }

    private static String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8);
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
