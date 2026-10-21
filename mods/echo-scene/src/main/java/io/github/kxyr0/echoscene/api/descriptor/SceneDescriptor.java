package io.github.kxyr0.echoscene.api.descriptor;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import java.util.List;
import java.util.Objects;

public record SceneDescriptor(
        SceneResourceId resourceId,
        SceneLocalId entry,
        List<SceneLocalId> scenes,
        List<SceneReference> references,
        List<SceneCapability> capabilityPlan
) {
    public SceneDescriptor {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        entry = Objects.requireNonNull(entry, "entry");
        scenes = List.copyOf(scenes);
        references = List.copyOf(references);
        capabilityPlan = List.copyOf(capabilityPlan);
    }
}
