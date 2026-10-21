package io.github.kxyr0.echosavemigration.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MigrationResourceAliasesTest {
    @Test void pendingReactionTargetResolvesThroughExistingRenameChain() {
        var first = new ResourceId("jdh:first_talk");
        var middle = new ResourceId("jdh:father_talk");
        var current = new ResourceId("jdh:father_first_talk");
        var aliases = MigrationResourceAliases.from(List.of(step(1, first, middle), step(2, middle, current)));
        assertEquals(current, aliases.canonical(first));
        assertEquals(current, aliases.canonical(current));
        assertThrows(MigrationPlanningException.class, () -> MigrationResourceAliases.from(
                List.of(step(1, first, middle), step(2, first, current))));
    }
    private MigrationDefinition step(int schema, ResourceId oldId, ResourceId newId) {
        return new MigrationDefinition(new MigrationId("jdh:rename_" + schema),
                new SaveSchemaVersion(schema), new SaveSchemaVersion(schema + 1), schema,
                List.of(new MigrationOperation.RenameResource(oldId, newId)));
    }
}
