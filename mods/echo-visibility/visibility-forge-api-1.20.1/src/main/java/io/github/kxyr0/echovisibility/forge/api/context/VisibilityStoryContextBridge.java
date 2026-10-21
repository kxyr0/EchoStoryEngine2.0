package io.github.kxyr0.echovisibility.forge.api.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class VisibilityStoryContextBridge {
    private static final int MAX_CONTEXTS = 256;
    private static final Map<String, Map<String, String>> CONTEXTS = new LinkedHashMap<>();

    private VisibilityStoryContextBridge() {
    }

    public static synchronized void install(String storyExecutionId, Map<String, String> values) {
        CONTEXTS.put(require(storyExecutionId), Map.copyOf(Objects.requireNonNull(values, "values")));
        while (CONTEXTS.size() > MAX_CONTEXTS) {
            CONTEXTS.remove(CONTEXTS.keySet().iterator().next());
        }
    }

    public static synchronized Optional<Map<String, String>> find(String storyExecutionId) {
        return Optional.ofNullable(CONTEXTS.get(require(storyExecutionId)));
    }

    public static Map<String, String> merge(String storyExecutionId, Map<String, String> explicit) {
        Map<String, String> merged = new LinkedHashMap<>();
        find(storyExecutionId).ifPresent(merged::putAll);
        merged.putAll(Objects.requireNonNull(explicit, "explicit"));
        return Map.copyOf(merged);
    }

    public static synchronized void clear(String storyExecutionId) {
        CONTEXTS.remove(require(storyExecutionId));
    }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "storyExecutionId").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("Story execution id must not be blank.");
        }
        return checked;
    }
}
