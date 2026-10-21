package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import java.util.List;
import java.util.Objects;

/**
 * Compiled immutable face rig definition.
 */
public record FaceRigDefinition(
        FaceRigId id,
        BoneHierarchy hierarchy,
        List<FaceLayerDefinition> layers,
        List<FaceControlDefinition> controls,
        String semanticFingerprint
) {
    public FaceRigDefinition {
        id = Objects.requireNonNull(id, "id");
        hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
        controls = List.copyOf(Objects.requireNonNull(controls, "controls"));
        semanticFingerprint = Objects.requireNonNull(semanticFingerprint, "semanticFingerprint");
    }
}

