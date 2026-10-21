package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AnimationSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_animation/") && value.endsWith(".es")
                && !value.contains("/portrait_overlays/");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        AnimationCompileResult result = new EchoScriptAnimationCompiler().compile(sourceId, sourceText);
        List<ContentSourceProblem> problems = result.diagnostics().stream()
                .filter(value -> value.severity() != DiagnosticSeverity.INFO)
                .map(value -> new ContentSourceProblem(
                        value.severity() == DiagnosticSeverity.ERROR ? ContentSeverity.ERROR : ContentSeverity.WARNING,
                        value.code(), SourceIdentities.inferred(sourceId), sourceId, sourceText, 1, 1,
                        value.message())).toList();
        List<ContentDescriptor> descriptors = new ArrayList<>();
        result.faceRig().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText, value.id().value(),
                ContentResourceKind.ANIMATION_RIG, List.of())));
        result.facePose().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText, value.id().value(),
                ContentResourceKind.FACE_POSE, refs(sourceId, sourceText, ContentReferenceKind.ANIMATION_RIG,
                        value.rigId().value()))));
        result.facialAnimation().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText,
                value.id().value(), ContentResourceKind.FACE_ANIMATION, value.rigId().map(id -> refs(sourceId,
                        sourceText, ContentReferenceKind.ANIMATION_RIG, id.value())).orElse(List.of()))));
        result.animationRig().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText,
                value.id().value(), ContentResourceKind.ANIMATION_RIG, List.of())));
        result.animationClip().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText, value.id().value(),
                ContentResourceKind.ANIMATION_CLIP, value.rigId().map(id -> refs(sourceId, sourceText,
                        ContentReferenceKind.ANIMATION_RIG, id.value())).orElse(List.of()))));
        result.animationDefinition().ifPresent(value -> {
            List<ContentReference> refs = new ArrayList<>();
            value.entryVariants().forEach(entry -> refs.add(ref(sourceId, sourceText,
                    ContentReferenceKind.ANIMATION_CLIP, entry.clipId().value())));
            refs.add(ref(sourceId, sourceText, ContentReferenceKind.ANIMATION_CLIP, value.main().clipId().value()));
            value.exitVariants().forEach(exit -> refs.add(ref(sourceId, sourceText,
                    ContentReferenceKind.ANIMATION_CLIP, exit.clipId().value())));
            descriptors.add(descriptor(sourceId, sourceText, value.id().value(), ContentResourceKind.ANIMATION, refs));
        });
        result.animationProfile().ifPresent(value -> descriptors.add(descriptor(sourceId, sourceText,
                value.id().value(), ContentResourceKind.ANIMATION_PROFILE, refs(sourceId, sourceText,
                        ContentReferenceKind.ANIMATION_RIG, value.rigId().value()))));
        return new AdaptedSource(descriptors, problems);
    }

    private ContentDescriptor descriptor(String sourceId, String text, String id, ContentResourceKind kind,
            List<ContentReference> references) {
        return ContentDescriptor.simple(new ContentResourceId(id), kind,
                SourceSpans.file(sourceId, text), references);
    }

    private List<ContentReference> refs(String sourceId, String text, ContentReferenceKind kind, String id) {
        return List.of(ref(sourceId, text, kind, id));
    }

    private ContentReference ref(String sourceId, String text, ContentReferenceKind kind, String id) {
        return new ContentReference(kind, new ContentResourceId(id),
                SourceSpans.find(sourceId, text, id));
    }
}
