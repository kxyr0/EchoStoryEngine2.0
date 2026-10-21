package io.github.kxyr0.echoplayersleep.forge.api.catalog;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import java.util.List;

public interface PlayerSleepScenarioCatalogView {
    List<PlayerSleepScenarioId> scenarioIds();

    boolean contains(PlayerSleepScenarioId scenarioId);
}
