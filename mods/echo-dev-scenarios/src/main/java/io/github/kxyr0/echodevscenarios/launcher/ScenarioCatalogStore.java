package io.github.kxyr0.echodevscenarios.launcher;

import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ScenarioCatalogStore {
    private final Map<ScenarioId, ScenarioDefinition> scenarios = new LinkedHashMap<>();
    private final Map<ScenarioId, String> errors = new LinkedHashMap<>();
    private final ArrayDeque<ScenarioId> recent = new ArrayDeque<>();

    public void install(Collection<ScenarioDefinition> values, Map<ScenarioId, String> validationErrors) {
        scenarios.clear();
        values.stream().sorted((left, right) -> left.id().compareTo(right.id()))
                .forEach(value -> scenarios.put(value.id(), value));
        errors.clear();
        errors.putAll(validationErrors);
    }

    public Optional<ScenarioDefinition> find(ScenarioId id) { return Optional.ofNullable(scenarios.get(id)); }
    public Optional<String> error(ScenarioId id) { return Optional.ofNullable(errors.get(id)); }
    public List<ScenarioDefinition> list() { return List.copyOf(scenarios.values()); }

    public List<ScenarioDefinition> search(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return scenarios.values().stream().filter(value -> value.id().value().contains(normalized)
                || value.title().toLowerCase(Locale.ROOT).contains(normalized)
                || value.target().resourceId().contains(normalized)).toList();
    }

    public void launched(ScenarioId id) {
        recent.remove(id);
        recent.addFirst(id);
        while (recent.size() > 5) recent.removeLast();
    }

    public List<ScenarioId> recent() { return List.copyOf(recent); }
}
