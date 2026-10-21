package io.github.kxyr0.echoscene.api.definition;

import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import java.util.List;
import java.util.Objects;

public record SceneDefinition(
        SceneResourceId resourceId,
        SceneLocalId entry,
        SceneRevision revision,
        List<SceneStage> stages
) {
    public SceneDefinition {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        entry = Objects.requireNonNull(entry, "entry");
        revision = Objects.requireNonNull(revision, "revision");
        stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
    }

    public List<SceneCapability> capabilityPlan() {
        return stages.stream().flatMap(stage -> stage.commands().stream())
                .flatMap(command -> command.capabilities().stream()).distinct().sorted().toList();
    }
}
