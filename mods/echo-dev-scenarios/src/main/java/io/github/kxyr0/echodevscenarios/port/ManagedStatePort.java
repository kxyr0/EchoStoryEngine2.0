package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ManagedStateSnapshot;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;

public interface ManagedStatePort {
    ManagedStateSnapshot capture(ScenarioSessionId owner);

    void restore(ScenarioSessionId owner, ManagedStateSnapshot snapshot);
}
