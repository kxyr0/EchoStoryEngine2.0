package io.github.kxyr0.echoplayersleep.core.scenario;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSleepScenarioCatalog {
    private final Map<PlayerSleepScenarioId, PlayerSleepScenarioDefinition> scenarios = new LinkedHashMap<>();

    public boolean install(PlayerSleepScenarioDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (scenarios.containsKey(definition.scenarioId())) {
            return false;
        }
        scenarios.put(definition.scenarioId(), definition);
        return true;
    }

    public void replaceAll(List<PlayerSleepScenarioDefinition> definitions) {
        scenarios.clear();
        definitions.forEach(this::install);
    }

    public Optional<PlayerSleepScenarioDefinition> find(PlayerSleepScenarioId scenarioId) {
        return Optional.ofNullable(scenarios.get(Objects.requireNonNull(scenarioId, "scenarioId")));
    }

    public List<PlayerSleepScenarioDefinition> all() {
        return List.copyOf(scenarios.values());
    }
}
