package io.github.kxyr0.echoevents.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

final class TransientCausalCounts {
    private final Map<String, Integer> counts = new LinkedHashMap<>(256, 0.75F, true);
    int get(String key) { return counts.getOrDefault(key, 0); }
    void accepted(String key) {
        counts.merge(key, 1, Integer::sum);
        while (counts.size() > 4096) counts.remove(counts.keySet().iterator().next());
    }
}
