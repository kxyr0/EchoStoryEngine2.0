package io.github.kxyr0.echotrigger.core.persistence;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TriggerRecoveryLoader {
    public Map<TriggerId, Long> duplicateCounts(TriggerPersistenceImage image) {
        Objects.requireNonNull(image, "image");
        Map<TriggerId, Long> counts = new LinkedHashMap<>();
        image.instances().forEach(state -> counts.merge(state.triggerId(), 1L, Long::sum));
        counts.replaceAll((id, count) -> Math.max(0L, count - 1L));
        return Map.copyOf(counts);
    }
}
