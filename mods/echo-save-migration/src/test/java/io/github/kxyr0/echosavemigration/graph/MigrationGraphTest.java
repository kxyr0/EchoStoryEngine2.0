package io.github.kxyr0.echosavemigration.graph;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrationGraphTest {
    @Test
    void plansExactMultiStepPath() {
        MigrationGraph graph = new MigrationGraph(List.of(step(14, 15), step(15, 16), step(16, 17)));

        MigrationPlan plan = graph.plan(version(14), version(17));

        assertEquals(List.of(15, 16, 17), plan.steps().stream().map(value -> value.to().value()).toList());
    }

    @Test
    void refusesMissingAmbiguousAndDowngradePaths() {
        MigrationGraph missing = new MigrationGraph(List.of(step(14, 15)));
        MigrationGraph ambiguous = new MigrationGraph(List.of(step(14, 15), step(14, 16)));

        assertEquals("SAVE_MIGRATION_PATH_MISSING", assertThrows(MigrationPlanningException.class,
                () -> missing.plan(version(14), version(16))).code());
        assertEquals("SAVE_MIGRATION_PATH_AMBIGUOUS", assertThrows(MigrationPlanningException.class,
                () -> ambiguous.plan(version(14), version(16))).code());
        assertEquals("SAVE_SCHEMA_NEWER_THAN_RUNTIME", assertThrows(MigrationPlanningException.class,
                () -> missing.plan(version(17), version(16))).code());
    }

    private MigrationDefinition step(int from, int to) {
        return new MigrationDefinition(new MigrationId("jdh:" + from + "_to_" + to), version(from), version(to),
                to, List.of());
    }

    private SaveSchemaVersion version(int value) {
        return new SaveSchemaVersion(value);
    }
}
