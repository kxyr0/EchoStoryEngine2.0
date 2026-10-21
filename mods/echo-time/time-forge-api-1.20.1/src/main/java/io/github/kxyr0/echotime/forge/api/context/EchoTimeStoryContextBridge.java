package io.github.kxyr0.echotime.forge.api.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EchoTimeStoryContextBridge {
    private static final int CAPACITY = 512;
    private static final Map<String, Map<String, String>> CONTEXTS = new LinkedHashMap<>();

    private EchoTimeStoryContextBridge() { }

    public static synchronized void install(String executionId, Map<String, String> values) {
        String id = require(executionId);
        CONTEXTS.remove(id);
        CONTEXTS.put(id, Map.copyOf(Objects.requireNonNull(values, "values")));
        while (CONTEXTS.size() > CAPACITY) CONTEXTS.remove(CONTEXTS.keySet().iterator().next());
    }

    public static synchronized Optional<Map<String, String>> find(String executionId) {
        return Optional.ofNullable(CONTEXTS.get(require(executionId)));
    }

    public static synchronized Map<String, String> merge(String executionId, Map<String, String> explicit) {
        Map<String, String> values = new LinkedHashMap<>();
        find(executionId).ifPresent(values::putAll);
        values.putAll(Objects.requireNonNull(explicit, "explicit"));
        return Map.copyOf(values);
    }

    public static synchronized void clear(String executionId) { CONTEXTS.remove(require(executionId)); }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "executionId").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("executionId cannot be blank.");
        return checked;
    }
}
