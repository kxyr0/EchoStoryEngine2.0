package io.github.kxyr0.echosavemigration.graph;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MigrationGraph {
    private final Map<SaveSchemaVersion, List<MigrationDefinition>> outgoing;

    public MigrationGraph(List<MigrationDefinition> definitions) {
        Map<SaveSchemaVersion, List<MigrationDefinition>> indexed = new HashMap<>();
        definitions.forEach(value -> indexed.computeIfAbsent(value.from(), ignored -> new ArrayList<>()).add(value));
        indexed.replaceAll((ignored, values) -> values.stream().sorted((a, b) -> a.id().compareTo(b.id())).toList());
        outgoing = Map.copyOf(indexed);
    }

    public MigrationPlan plan(SaveSchemaVersion source, SaveSchemaVersion target) {
        if (source.compareTo(target) > 0) {
            throw error("SAVE_SCHEMA_NEWER_THAN_RUNTIME", "Save schema is newer than runtime.");
        }
        List<MigrationDefinition> steps = new ArrayList<>();
        Set<SaveSchemaVersion> visited = new HashSet<>();
        SaveSchemaVersion cursor = source;
        while (!cursor.equals(target)) {
            if (!visited.add(cursor)) {
                throw error("SAVE_MIGRATION_CYCLE", "Migration cycle at schema " + cursor.value());
            }
            List<MigrationDefinition> choices = outgoing.getOrDefault(cursor, List.of()).stream()
                    .filter(value -> value.to().compareTo(target) <= 0).toList();
            if (choices.isEmpty()) {
                throw error("SAVE_MIGRATION_PATH_MISSING", "No migration from schema " + cursor.value());
            }
            if (choices.size() != 1) {
                throw error("SAVE_MIGRATION_PATH_AMBIGUOUS", "Multiple migrations from schema " + cursor.value());
            }
            MigrationDefinition step = choices.get(0);
            steps.add(step);
            cursor = step.to();
        }
        return new MigrationPlan(source, target, steps);
    }

    public List<MigrationDefinition> definitions() {
        return outgoing.values().stream().flatMap(List::stream).toList();
    }

    private MigrationPlanningException error(String code, String message) {
        return new MigrationPlanningException(code, message);
    }
}
