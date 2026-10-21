package io.github.kxyr0.echoseating.core.catalog;

import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SeatZoneSpatialIndex {
    private final Map<String, List<SeatZoneDefinition>> cells;

    public SeatZoneSpatialIndex(List<SeatZoneDefinition> zones) {
        Map<String, List<SeatZoneDefinition>> indexed = new LinkedHashMap<>();
        zones.forEach(zone -> index(zone, indexed));
        cells = indexed.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())));
    }

    public List<SeatZoneDefinition> candidates(DimensionId dimension, BlockPosition position) {
        return cells.getOrDefault(key(dimension, chunk(position.x()), chunk(position.z())), List.of());
    }

    private static void index(
            SeatZoneDefinition zone,
            Map<String, List<SeatZoneDefinition>> indexed) {
        int minChunkX = chunk(zone.area().min().x());
        int maxChunkX = chunk(zone.area().max().x());
        int minChunkZ = chunk(zone.area().min().z());
        int maxChunkZ = chunk(zone.area().max().z());
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                indexed.computeIfAbsent(key(zone.dimension(), chunkX, chunkZ), ignored -> new ArrayList<>())
                        .add(zone);
            }
        }
    }

    private static int chunk(int coordinate) {
        return Math.floorDiv(coordinate, 16);
    }

    private static String key(DimensionId dimension, int chunkX, int chunkZ) {
        return dimension.value() + "|" + chunkX + "|" + chunkZ;
    }
}
