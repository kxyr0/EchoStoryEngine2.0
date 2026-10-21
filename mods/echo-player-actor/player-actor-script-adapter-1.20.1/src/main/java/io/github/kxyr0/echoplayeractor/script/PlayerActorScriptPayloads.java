package io.github.kxyr0.echoplayeractor.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlayerActorScriptPayloads {
    private PlayerActorScriptPayloads() {
    }

    public static String encode(Map<String, String> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> escape(entry.getKey()) + "=" + escape(entry.getValue()))
                .collect(Collectors.joining(";"));
    }

    public static Map<String, String> decode(String payload) {
        Map<String, String> output = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        for (String part : payload.split(";")) {
            int equals = part.indexOf('=');
            if (equals > 0) {
                output.put(unescape(part.substring(0, equals)), unescape(part.substring(equals + 1)));
            }
        }
        return Map.copyOf(output);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\s").replace("=", "\\e");
    }

    private static String unescape(String value) {
        StringBuilder output = new StringBuilder();
        boolean escaped = false;
        for (char character : value.toCharArray()) {
            if (escaped) {
                output.append(character == 's' ? ';' : character == 'e' ? '=' : character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                output.append(character);
            }
        }
        return output.toString();
    }
}
