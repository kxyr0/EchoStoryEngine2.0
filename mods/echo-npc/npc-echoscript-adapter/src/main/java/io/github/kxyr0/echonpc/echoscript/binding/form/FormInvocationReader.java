package io.github.kxyr0.echonpc.echoscript.binding.form;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorDefinition;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorId;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyGeometry;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartId;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartTransform;
import io.github.kxyr0.echonpc.api.form.definition.NpcAppearanceDefinition;
import io.github.kxyr0.echonpc.api.form.definition.NpcAppearanceRevision;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FormInvocationReader {
    private static final Set<String> FORM_ARGS = Set.of("id", "skin", "model", "dimensions", "bodyScale",
            "parts", "anchors", "animationGraph", "faceProfile", "portrait", "tags");
    private static final String DEFAULT_ANIMATION_GRAPH = "echo_npc:humanoid_locomotion",
            DEFAULT_FACE_PROFILE = "echo_npc:humanoid_face";
    private static final Set<String> DIMENSION_ARGS = Set.of("width", "height", "eyeHeight");
    private static final Set<String> PART_ARGS = Set.of("id", "scale", "scaleX", "scaleY", "scaleZ",
            "pivotX", "pivotY", "pivotZ", "rotationX", "rotationY", "rotationZ");
    private static final Set<String> ANCHOR_ARGS = Set.of("id", "x", "y", "z");
    private final FormArgumentReader args = new FormArgumentReader();

    Optional<NpcFormDefinition> read(String sourceId, InvocationExpressionSyntax call,
            NpcFormRevision revision, List<NpcDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = args.named(call.arguments(), FORM_ARGS, diagnostics);
        Optional<NpcFormId> id = formId(args.string("id", named, diagnostics), diagnostics);
        Optional<NpcSkinId> skin = skin(named, diagnostics);
        String model = args.string("model", named, diagnostics);
        Optional<NpcBodyDimensions> dimensions = dimensions(named, diagnostics);
        NpcBodyGeometry geometry = geometry(named, diagnostics);
        String graph = args.optionalString("animationGraph", named, diagnostics).orElse(DEFAULT_ANIMATION_GRAPH);
        String face = args.optionalString("faceProfile", named, diagnostics).orElse(DEFAULT_FACE_PROFILE);
        String portrait = args.optionalString("portrait", named, diagnostics).or(() -> skin.map(NpcSkinId::value))
                .orElse(id.map(NpcFormId::value).orElse("echo_npc:portrait/default"));
        List<NpcBodyAnchorDefinition> anchors = anchors(named, dimensions, diagnostics);
        Set<String> tags = Set.copyOf(args.stringArray("tags", named, diagnostics));
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error) || id.isEmpty() || dimensions.isEmpty()) {
            return Optional.empty();
        }
        String fingerprint = fingerprint(new FingerprintInput(id.orElseThrow(), skin, model,
                dimensions.orElseThrow(), geometry, anchors, graph, face, portrait, tags));
        var appearance = new NpcAppearanceDefinition(skin, model, graph, face, portrait,
                new NpcAppearanceRevision(revision.value()));
        return Optional.of(new NpcFormDefinition(id.orElseThrow(), appearance, geometry, dimensions.orElseThrow(),
                anchors, tags, revision, sourceId, fingerprint));
    }

    private Optional<NpcSkinId> skin(Map<String, ArgumentSyntax> named, List<NpcDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> skinCall = args.call("skin", "Skin", named, diagnostics);
        if (skinCall.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new NpcSkinId(args.skinCall(skinCall.orElseThrow(), diagnostics)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_SKIN_NOT_FOUND",
                    "NpcSkinId имеет неверный формат.", "skin"));
            return Optional.empty();
        }
    }

    private Optional<NpcBodyDimensions> dimensions(Map<String, ArgumentSyntax> named,
            List<NpcDiagnostic> diagnostics) {
        Optional<InvocationExpressionSyntax> call = args.call("dimensions", "Dimensions", named, diagnostics);
        if (call.isEmpty()) {
            return Optional.empty();
        }
        Map<String, ArgumentSyntax> values = args.named(call.orElseThrow().arguments(), DIMENSION_ARGS, diagnostics);
        try {
            return Optional.of(new NpcBodyDimensions(args.number("width", values, 0.0D, diagnostics),
                    args.number("height", values, 0.0D, diagnostics),
                    args.number("eyeHeight", values, 0.0D, diagnostics)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_DIMENSIONS_INVALID", exception.getMessage(), "dimensions"));
            return Optional.empty();
        }
    }

    private NpcBodyGeometry geometry(Map<String, ArgumentSyntax> named, List<NpcDiagnostic> diagnostics) {
        double globalScale = args.number("bodyScale", named, 1.0D, diagnostics);
        List<NpcBodyPartTransform> parts = new ArrayList<>();
        for (InvocationExpressionSyntax partCall : args.callArray("parts", "Part", named, diagnostics)) {
            readPart(partCall, diagnostics).ifPresent(parts::add);
        }
        try {
            return new NpcBodyGeometry(globalScale, parts);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_PART_TRANSFORM_INVALID", exception.getMessage(), "parts"));
            return new NpcBodyGeometry(1.0D, List.of());
        }
    }

    private Optional<NpcBodyPartTransform> readPart(InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = args.named(call.arguments(), PART_ARGS, diagnostics);
        Optional<NpcBodyPartId> id = partId(args.string("id", named, diagnostics), diagnostics);
        double scale = args.number("scale", named, 1.0D, diagnostics);
        try {
            return id.map(value -> new NpcBodyPartTransform(value,
                    args.number("scaleX", named, scale, diagnostics),
                    args.number("scaleY", named, scale, diagnostics),
                    args.number("scaleZ", named, scale, diagnostics),
                    args.number("pivotX", named, 0.0D, diagnostics),
                    args.number("pivotY", named, 0.0D, diagnostics),
                    args.number("pivotZ", named, 0.0D, diagnostics),
                    args.number("rotationX", named, 0.0D, diagnostics),
                    args.number("rotationY", named, 0.0D, diagnostics),
                    args.number("rotationZ", named, 0.0D, diagnostics)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_PART_TRANSFORM_INVALID", exception.getMessage(), "Part"));
            return Optional.empty();
        }
    }

    private List<NpcBodyAnchorDefinition> anchors(Map<String, ArgumentSyntax> named,
            Optional<NpcBodyDimensions> dimensions, List<NpcDiagnostic> diagnostics) {
        List<NpcBodyAnchorDefinition> anchors = new ArrayList<>();
        for (InvocationExpressionSyntax anchorCall : args.callArray("anchors", "Anchor", named, diagnostics)) {
            readAnchor(anchorCall, diagnostics).ifPresent(anchors::add);
        }
        if (anchors.isEmpty() && dimensions.isPresent()) {
            return defaultAnchors(dimensions.orElseThrow());
        }
        return List.copyOf(anchors);
    }

    private Optional<NpcBodyAnchorDefinition> readAnchor(InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = args.named(call.arguments(), ANCHOR_ARGS, diagnostics);
        try {
            return Optional.of(new NpcBodyAnchorDefinition(new NpcBodyAnchorId(args.string("id", named, diagnostics)),
                    args.number("x", named, 0.0D, diagnostics), args.number("y", named, 0.0D, diagnostics),
                    args.number("z", named, 0.0D, diagnostics)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_ANCHOR_INVALID", exception.getMessage(), "Anchor"));
            return Optional.empty();
        }
    }

    private Optional<NpcFormId> formId(String value, List<NpcDiagnostic> diagnostics) {
        try {
            return Optional.of(new NpcFormId(value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_DEFINITION_INVALID", "NpcFormId неверен.", value));
            return Optional.empty();
        }
    }

    private Optional<NpcBodyPartId> partId(String value, List<NpcDiagnostic> diagnostics) {
        try {
            return Optional.of(NpcBodyPartId.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_PART_TRANSFORM_INVALID", "Unknown body part.", value));
            return Optional.empty();
        }
    }

    private List<NpcBodyAnchorDefinition> defaultAnchors(NpcBodyDimensions dimensions) {
        return List.of(new NpcBodyAnchorDefinition(NpcBodyAnchorId.FEET, 0.0D, 0.0D, 0.0D),
                new NpcBodyAnchorDefinition(NpcBodyAnchorId.CHEST, 0.0D, dimensions.height() * 0.62D, 0.0D),
                new NpcBodyAnchorDefinition(NpcBodyAnchorId.HEAD, 0.0D, dimensions.eyeHeight() + 0.08D, 0.0D),
                new NpcBodyAnchorDefinition(NpcBodyAnchorId.EYES, 0.0D, dimensions.eyeHeight(), 0.0D));
    }

    private String fingerprint(FingerprintInput input) {
        String sortedTags = input.tags().stream().sorted().toList().toString();
        return FormFingerprint.sha256("form:v1\nid=" + input.id().value() + "\nskin="
                + input.skin().map(NpcSkinId::value).orElse("<none>") + "\nmodel=" + input.model()
                + "\ndim=" + input.dimensions() + "\ngeometry=" + input.geometry()
                + "\nanchors=" + input.anchors() + "\ngraph=" + input.graph() + "\nface=" + input.face()
                + "\nportrait=" + input.portrait() + "\ntags=" + sortedTags);
    }

    private record FingerprintInput(
            NpcFormId id,
            Optional<NpcSkinId> skin,
            String model,
            NpcBodyDimensions dimensions,
            NpcBodyGeometry geometry,
            List<NpcBodyAnchorDefinition> anchors,
            String graph,
            String face,
            String portrait,
            Set<String> tags) {
    }
}
