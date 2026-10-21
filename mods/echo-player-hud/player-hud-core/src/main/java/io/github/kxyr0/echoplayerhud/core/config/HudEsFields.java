package io.github.kxyr0.echoplayerhud.core.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

record HudEsFields(Map<String, String> values) {
    static HudEsFields parse(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String entry : split(body)) {
            int colon = entry.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = entry.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = entry.substring(colon + 1).strip();
            values.put(key, unquote(value));
        }
        return new HudEsFields(values);
    }

    Optional<String> value(String... keys) {
        for (String key : keys) {
            String value = values.get(key.toLowerCase(Locale.ROOT));
            if (value != null && !value.isBlank()) {
                return Optional.of(value.strip());
            }
        }
        return Optional.empty();
    }

    String valueOr(String fallback, String... keys) {
        return value(keys).orElse(fallback);
    }

    private static ListBuilder split(String body) {
        ListBuilder entries = new ListBuilder();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        int squareDepth = 0;
        for (int index = 0; index < body.length(); index++) {
            char ch = body.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (ch == '\\' && quoted) {
                escaped = true;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (!quoted && ch == '[') {
                squareDepth++;
            } else if (!quoted && ch == ']') {
                squareDepth = Math.max(0, squareDepth - 1);
            } else if (!quoted && squareDepth == 0 && ch == ',') {
                entries.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        entries.add(current.toString());
        return entries;
    }

    private static String unquote(String raw) {
        String value = raw.strip();
        while (value.endsWith(";") || value.endsWith(",")) {
            value = value.substring(0, value.length() - 1).strip();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"");
        }
        return value;
    }

    static final class ListBuilder extends java.util.ArrayList<String> {
        @Override
        public boolean add(String value) {
            String normalized = value.strip();
            return !normalized.isEmpty() && super.add(normalized);
        }
    }
}
