package io.github.kxyr0.echosavemigration.graph;

import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import java.util.List;

public final class MigrationResourceAliases {
    private MigrationResourceAliases() { }
    public static AliasGraph from(List<MigrationDefinition> definitions) {
        AliasGraph graph = new AliasGraph();
        definitions.stream().flatMap(value -> value.operations().stream())
                .filter(MigrationOperation.RenameResource.class::isInstance)
                .map(MigrationOperation.RenameResource.class::cast)
                .forEach(value -> graph.formerly(value.oldId(), value.newId()));
        return graph;
    }
}
