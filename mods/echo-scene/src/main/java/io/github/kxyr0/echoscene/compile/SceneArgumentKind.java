package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.api.descriptor.SceneReferenceKind;
import java.util.Optional;

public enum SceneArgumentKind {
    VALUE(null),
    ACTOR(SceneReferenceKind.ACTOR),
    LOCATION(SceneReferenceKind.LOCATION),
    CAMERA(SceneReferenceKind.CAMERA),
    DIALOGUE(SceneReferenceKind.DIALOGUE),
    ANIMATION(SceneReferenceKind.ANIMATION),
    SCENE(SceneReferenceKind.SCENE),
    EFFECT(SceneReferenceKind.EFFECT),
    OBSERVATION(SceneReferenceKind.OBSERVATION),
    ITEM(SceneReferenceKind.ITEM);

    private final SceneReferenceKind referenceKind;

    SceneArgumentKind(SceneReferenceKind referenceKind) {
        this.referenceKind = referenceKind;
    }

    public Optional<SceneReferenceKind> referenceKind() {
        return Optional.ofNullable(referenceKind);
    }
}
