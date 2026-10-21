package io.github.kxyr0.echoscene.runtime;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record SceneCheckpoint(
        SceneResourceId resourceId,
        SceneLocalId entry,
        SceneRevision revision,
        SceneExecutionId executionId,
        String rootOwner,
        SceneExecutionState state,
        int stageIndex,
        Set<SceneStepId> completed,
        Set<SceneStepId> pending,
        List<SceneCapability> held,
        Optional<String> failure
) {
    public SceneCheckpoint {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        entry = Objects.requireNonNull(entry, "entry");
        revision = Objects.requireNonNull(revision, "revision");
        executionId = Objects.requireNonNull(executionId, "executionId");
        rootOwner = Objects.requireNonNull(rootOwner, "rootOwner");
        state = Objects.requireNonNull(state, "state");
        completed = Set.copyOf(completed);
        pending = Set.copyOf(pending);
        held = List.copyOf(held);
        failure = Objects.requireNonNull(failure, "failure");
    }
}
