package io.github.kxyr0.echoscene.api.model;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SceneInspection(
        SceneResourceId resourceId,
        SceneLocalId entry,
        SceneExecutionId executionId,
        SceneExecutionState state,
        int stageIndex,
        List<SceneStepId> waiting,
        List<SceneCapability> heldLeases,
        Optional<String> failure
) {
    public SceneInspection {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        entry = Objects.requireNonNull(entry, "entry");
        executionId = Objects.requireNonNull(executionId, "executionId");
        state = Objects.requireNonNull(state, "state");
        waiting = List.copyOf(waiting);
        heldLeases = List.copyOf(heldLeases);
        failure = Objects.requireNonNull(failure, "failure");
    }
}
