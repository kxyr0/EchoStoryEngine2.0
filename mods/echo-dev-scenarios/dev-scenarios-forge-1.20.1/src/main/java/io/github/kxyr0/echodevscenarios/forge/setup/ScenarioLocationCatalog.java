package io.github.kxyr0.echodevscenarios.forge.setup;

import java.util.Optional;

public interface ScenarioLocationCatalog {
    Optional<ScenarioLocation> location(String id);
}
