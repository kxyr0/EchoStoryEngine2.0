package io.github.kxyr0.echoobservation.forge.client.catalog;

import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ObservationSpatialIndex {
    private final Map<String, Map<Long, List<ObservationDefinition>>> buckets = new LinkedHashMap<>();

    public void rebuild(ObservationCatalogSnapshot catalog, Set<String> activeResources,
            Set<String> disabled, Set<String> completed) {
        buckets.clear();
        catalog.resources().stream().filter(resource -> activeResources.contains(resource.id().toString()))
                .flatMap(resource -> resource.observations().stream())
                .filter(value -> value.placed() && !disabled.contains(value.id().toString())
                        && (!completed.contains(value.id().toString()) || value.repeatable()))
                .forEach(this::add);
    }

    public List<ObservationDefinition> nearby(String dimension, double worldX, double worldZ) {
        Map<Long, List<ObservationDefinition>> dimensionBuckets = buckets.get(dimension);
        if (dimensionBuckets == null) return List.of();
        int chunkX = floorChunk(worldX);
        int chunkZ = floorChunk(worldZ);
        List<ObservationDefinition> result = new ArrayList<>();
        for (int x = chunkX - 8; x <= chunkX + 8; x++) {
            for (int z = chunkZ - 8; z <= chunkZ + 8; z++) {
                result.addAll(dimensionBuckets.getOrDefault(key(x, z), List.of()));
            }
        }
        return result;
    }

    public int size() {
        return buckets.values().stream().flatMap(value -> value.values().stream())
                .mapToInt(List::size).sum();
    }

    private void add(ObservationDefinition definition) {
        var anchor = definition.anchor().orElseThrow();
        buckets.computeIfAbsent(anchor.dimension(), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(key(anchor.blockX() >> 4, anchor.blockZ() >> 4), ignored -> new ArrayList<>())
                .add(definition);
    }

    private int floorChunk(double value) { return ((int) Math.floor(value)) >> 4; }
    private long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
}
