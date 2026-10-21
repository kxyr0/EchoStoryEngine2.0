package io.github.kxyr0.echoseating.forge.api.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SeatingStoryContextBridge {
    private static final Map<String, Map<String, String>> CONTEXTS = new ConcurrentHashMap<>();

    private SeatingStoryContextBridge() {
    }

    public static void install(String storyExecutionId, Map<String, String> values) {
        CONTEXTS.put(requireText(storyExecutionId, "storyExecutionId"),
                Map.copyOf(Objects.requireNonNull(values, "values")));
    }

    public static Optional<Map<String, String>> find(String storyExecutionId) {
        return Optional.ofNullable(CONTEXTS.get(requireText(storyExecutionId, "storyExecutionId")));
    }

    public static Map<String, String> merge(String storyExecutionId, Map<String, String> explicitValues) {
        Map<String, String> merged = new LinkedHashMap<>();
        find(storyExecutionId).ifPresent(merged::putAll);
        merged.putAll(Objects.requireNonNull(explicitValues, "explicitValues"));
        return Map.copyOf(merged);
    }

    public static void clear(String storyExecutionId) {
        CONTEXTS.remove(requireText(storyExecutionId, "storyExecutionId"));
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
