package io.github.kxyr0.echosavemigration.graph;

import io.github.kxyr0.echosavemigration.api.ResourceId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AliasGraph {
    private final Map<ResourceId, ResourceId> aliases = new HashMap<>();

    public void formerly(ResourceId oldId, ResourceId currentId) {
        ResourceId existing = aliases.putIfAbsent(oldId, currentId);
        if (existing != null && !existing.equals(currentId)) {
            throw new MigrationPlanningException("SAVE_MIGRATION_PATH_AMBIGUOUS",
                    "Formerly id " + oldId.value() + " has multiple targets.");
        }
        canonical(oldId);
    }

    public ResourceId canonical(ResourceId id) {
        Set<ResourceId> visited = new HashSet<>();
        ResourceId cursor = id;
        while (aliases.containsKey(cursor)) {
            if (!visited.add(cursor)) {
                throw new MigrationPlanningException("SAVE_MIGRATION_CYCLE", "Formerly cycle at " + id.value());
            }
            cursor = aliases.get(cursor);
        }
        return cursor;
    }

    public Map<ResourceId, ResourceId> snapshot() {
        Map<ResourceId, ResourceId> collapsed = new HashMap<>();
        aliases.keySet().forEach(oldId -> collapsed.put(oldId, canonical(oldId)));
        return Map.copyOf(collapsed);
    }
}
