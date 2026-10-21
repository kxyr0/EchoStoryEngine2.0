package io.github.kxyr0.echodevscenarios.compile;

import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import java.util.List;

public interface ScenarioCatalog {
    List<ScenarioSymbol> symbols();

    List<ScenarioTarget> targets(String resourceId);

    default List<ScenarioSymbol> resolve(String name, ScenarioSymbolKind kind) {
        return symbols().stream().filter(value -> value.name().equals(name) && value.kind() == kind).toList();
    }

    default List<ScenarioSymbol> resolveResource(String id, ScenarioSymbolKind kind) {
        return symbols().stream().filter(value -> value.resourceId().equals(id) && value.kind() == kind).toList();
    }
}
