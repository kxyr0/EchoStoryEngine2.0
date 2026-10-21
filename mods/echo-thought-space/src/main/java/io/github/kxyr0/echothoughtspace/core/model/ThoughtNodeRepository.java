package io.github.kxyr0.echothoughtspace.core.model;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ThoughtNodeRepository {
    private final DeterministicThoughtLayout layout = new DeterministicThoughtLayout();
    private final Map<ThoughtNodeId, Integer> slots = new LinkedHashMap<>();
    private long fingerprint = Long.MIN_VALUE;
    private List<ThoughtCanvasNode> cached = List.of();

    public List<ThoughtCanvasNode> refresh(List<ThoughtNodeProvider> providers) {
        return refresh(providers, Map.of());
    }

    public List<ThoughtCanvasNode> refresh(List<ThoughtNodeProvider> providers,
            Map<ThoughtNodeId, ThoughtCanvasPosition> positionOverrides) {
        long current = fingerprint(providers, positionOverrides);
        if (current == fingerprint) return cached;
        Map<ThoughtNodeId, ThoughtNodeSnapshot> unique = new LinkedHashMap<>();
        providers.stream().sorted(Comparator.comparing(ThoughtNodeProvider::providerId)).forEach(provider ->
                provider.nodes().stream().sorted(Comparator.comparing(ThoughtNodeSnapshot::id))
                        .forEach(node -> unique.putIfAbsent(node.id(), node)));
        List<ThoughtCanvasNode> next = new ArrayList<>(unique.size());
        unique.values().forEach(node -> {
            int slot = slots.computeIfAbsent(node.id(), ignored -> slots.size());
            var point = layout.position(slot);
            ThoughtCanvasPosition position = positionOverrides.getOrDefault(node.id(),
                    new ThoughtCanvasPosition(point.x(), point.y()));
            next.add(new ThoughtCanvasNode(node, position));
        });
        cached = List.copyOf(next);
        fingerprint = current;
        return cached;
    }

    private static long fingerprint(List<ThoughtNodeProvider> providers,
            Map<ThoughtNodeId, ThoughtCanvasPosition> positionOverrides) {
        long result = 1L;
        for (ThoughtNodeProvider provider : providers.stream()
                .sorted(Comparator.comparing(ThoughtNodeProvider::providerId)).toList()) {
            result = 31L * result + provider.providerId().hashCode();
            result = 31L * result + provider.revision();
        }
        for (var entry : positionOverrides.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            result = 31L * result + entry.getKey().hashCode();
            result = 31L * result + entry.getValue().hashCode();
        }
        return result;
    }
}
