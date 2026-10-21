package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.descriptor.SceneDescriptor;
import io.github.kxyr0.echoscene.api.descriptor.SceneReference;
import io.github.kxyr0.echoscene.api.descriptor.SceneReferenceKind;
import java.util.List;

public final class SceneDescriptorAdapter {
    public ContentDescriptor adapt(SceneDescriptor scene) {
        SourceSpan span = scene.references().stream().findFirst().map(SceneReference::span).orElseGet(() -> origin(scene));
        List<ContentReference> references = scene.references().stream().map(reference -> new ContentReference(
                kind(reference, scene), new ContentResourceId(reference.target()), reference.span())).toList();
        return ContentDescriptor.simple(new ContentResourceId(scene.resourceId().value()),
                ContentResourceKind.SCENE, span, references);
    }

    private ContentReferenceKind kind(SceneReference reference, SceneDescriptor scene) {
        if (reference.kind() == SceneReferenceKind.ACTOR) {
            boolean player = scene.capabilityPlan().stream().anyMatch(capability ->
                    capability.kind() == SceneCapabilityKind.PLAYER && capability.resource().equals(reference.target()));
            return player ? ContentReferenceKind.PLAYER : ContentReferenceKind.NPC;
        }
        return switch (reference.kind()) {
            case LOCATION -> ContentReferenceKind.LOCATION;
            case CAMERA -> ContentReferenceKind.CAMERA;
            case DIALOGUE -> ContentReferenceKind.DIALOGUE;
            case ANIMATION -> ContentReferenceKind.ANIMATION;
            case SCENE -> ContentReferenceKind.SCENE;
            case EFFECT -> ContentReferenceKind.EFFECT;
            case OBSERVATION -> ContentReferenceKind.OBSERVATION;
            case ITEM -> ContentReferenceKind.ITEM;
            case AUDIO -> ContentReferenceKind.AUDIO;
            case ACTOR -> throw new IllegalStateException("Actor handled above.");
        };
    }

    private SourceSpan origin(SceneDescriptor scene) {
        SourcePosition point = new SourcePosition(0, 1, 1);
        return new SourceSpan(new SourceId(scene.resourceId().value()), 0, 0, point, point);
    }
}
