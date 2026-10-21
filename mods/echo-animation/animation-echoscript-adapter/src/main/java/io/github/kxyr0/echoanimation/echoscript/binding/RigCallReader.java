package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads FaceRig(...) semantics.
 */
final class RigCallReader {
    private final ArgumentReader arguments = new ArgumentReader();

    AnimationCompileResult read(InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            arguments.requireKnown(call, List.of("bones", "layers", "controls"), diagnostics);
            FaceRigId id = FaceRigId.parse(arguments.string(call, 0, null).orElseThrow());
            List<BoneDefinition> bones = arguments.calls(call, "bones").stream().map(this::bone).toList();
            List<FaceLayerDefinition> layers = arguments.calls(call, "layers").stream().map(this::layer).toList();
            List<FaceControlDefinition> controls = arguments.calls(call, "controls").stream()
                    .map(this::control)
                    .toList();
            FaceRigDefinition rig = new FaceRigDefinition(id, new BoneHierarchy(bones), layers, controls,
                    fingerprint(id, bones, layers, controls));
            new FaceRigValidator().validate(rig);
            return new AnimationCompileResult(Optional.of(rig), Optional.empty(), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-RIG", exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private BoneDefinition bone(InvocationExpressionSyntax call) {
        require(call, "Bone");
        BoneId id = BoneId.parse(arguments.string(call, 0, null).orElseThrow());
        Optional<BoneId> parent = arguments.string(call, -1, "parent").map(BoneId::parse);
        return new BoneDefinition(id, parent, transform(call));
    }

    private FaceLayerDefinition layer(InvocationExpressionSyntax call) {
        require(call, "Layer");
        FaceLayerId id = FaceLayerId.parse(arguments.string(call, 0, null).orElseThrow());
        BoneId bone = BoneId.parse(arguments.string(call, -1, "bone").orElseThrow());
        FaceTextureId texture = FaceTextureId.parse(arguments.string(call, -1, "texture").orElseThrow());
        LayerRole role = LayerRole.valueOf(arguments.string(call, -1, "role").orElse("DECORATION"));
        IntRect rect = rect(call, "", arguments.integer(call, -1, "w", 1), arguments.integer(call, -1, "h", 1));
        Optional<IntRect> clip = clip(call);
        return new FaceLayerDefinition(id, bone, texture, role, arguments.integer(call, -1, "order", 0),
                layerPlacement(call), rect, clip, arguments.bool(call, "visible", true));
    }

    private FaceControlDefinition control(InvocationExpressionSyntax call) {
        require(call, "BlinkControl");
        FaceControlId id = FaceControlId.parse(arguments.string(call, 0, null).orElseThrow());
        double upper = arguments.authoredNumber(call, -1, "upperTravel", 0);
        double lower = arguments.authoredNumber(call, -1, "lowerTravel", 0);
        Vec2 min = new Vec2(arguments.authoredNumber(call, -1, "gazeMinX", -1000),
                arguments.authoredNumber(call, -1, "gazeMinY", -1000));
        Vec2 max = new Vec2(arguments.authoredNumber(call, -1, "gazeMaxX", 1000),
                arguments.authoredNumber(call, -1, "gazeMaxY", 1000));
        BoneId upperBone = arguments.string(call, -1, "upperBone").map(BoneId::parse)
                .orElse(BoneId.parse("upper_eyelids"));
        BoneId lowerBone = arguments.string(call, -1, "lowerBone").map(BoneId::parse)
                .orElse(BoneId.parse("lower_eyelids"));
        return new FaceControlDefinition(id, upper, lower, min, max, upperBone, lowerBone,
                arguments.bool(call, "idle", false),
                arguments.integer(call, -1, "periodTicks", 80),
                arguments.integer(call, -1, "closeTicks", 3),
                arguments.integer(call, -1, "holdTicks", 1),
                arguments.integer(call, -1, "openTicks", 4));
    }

    private Transform3 transform(InvocationExpressionSyntax call) {
        return new Transform3(
                vector(call, ""),
                arguments.authoredNumber(call, -1, "rotationZ", 0),
                new Vec3(arguments.authoredNumber(call, -1, "scaleX", 1000),
                        arguments.authoredNumber(call, -1, "scaleY", 1000),
                        arguments.authoredNumber(call, -1, "scaleZ", 1000)),
                vector(call, "pivot"));
    }

    private Transform3 layerPlacement(InvocationExpressionSyntax call) {
        return new Transform3(
                vector(call, "place"),
                arguments.authoredNumber(call, -1, "rotationZ", 0),
                new Vec3(arguments.authoredNumber(call, -1, "scaleX", 1000),
                        arguments.authoredNumber(call, -1, "scaleY", 1000),
                        arguments.authoredNumber(call, -1, "scaleZ", 1000)),
                vector(call, "pivot"));
    }

    private Vec3 vector(InvocationExpressionSyntax call, String prefix) {
        return new Vec3(
                arguments.authoredNumber(call, -1, prefix.isEmpty() ? "x" : prefix + "X", 0),
                arguments.authoredNumber(call, -1, prefix.isEmpty() ? "y" : prefix + "Y", 0),
                arguments.authoredNumber(call, -1, prefix.isEmpty() ? "z" : prefix + "Z", 0));
    }

    private Optional<IntRect> clip(InvocationExpressionSyntax call) {
        if (arguments.string(call, -1, "clip").isPresent()) {
            return Optional.empty();
        }
        if (arguments.integer(call, -1, "clipW", 0) <= 0 || arguments.integer(call, -1, "clipH", 0) <= 0) {
            return Optional.empty();
        }
        return Optional.of(rect(call, "clip", arguments.integer(call, -1, "clipW", 1),
                arguments.integer(call, -1, "clipH", 1)));
    }

    private IntRect rect(InvocationExpressionSyntax call, String prefix, int width, int height) {
        String x = prefix.isEmpty() ? "x" : prefix + "X";
        String y = prefix.isEmpty() ? "y" : prefix + "Y";
        return new IntRect(arguments.integer(call, -1, x, 0), arguments.integer(call, -1, y, 0), width, height);
    }

    private String fingerprint(
            FaceRigId id,
            List<BoneDefinition> bones,
            List<FaceLayerDefinition> layers,
            List<FaceControlDefinition> controls
    ) {
        List<String> fields = new ArrayList<>();
        fields.add("id=" + id);
        bones.forEach(bone -> fields.add("bone=" + bone));
        layers.forEach(layer -> fields.add("layer=" + layer));
        controls.forEach(control -> fields.add("control=" + control));
        return new SemanticFingerprinter().fingerprint(fields);
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Ожидался intrinsic " + name + ".");
        }
    }
}
