package io.github.kxyr0.echodevscenarios.port;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.definition.MutationPolicy;
import io.github.kxyr0.echodevscenarios.definition.ScenarioSetupStep;

public record ScenarioSetupRequest(
        ScenarioSessionId owner,
        MutationPolicy mutationPolicy,
        String timeCrossingPolicy,
        ScenarioSetupStep step
) { }
