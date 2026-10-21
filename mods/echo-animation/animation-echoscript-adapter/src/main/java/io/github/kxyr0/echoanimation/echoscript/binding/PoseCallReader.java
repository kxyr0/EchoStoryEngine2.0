package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.id.FacePoseId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.definition.resource.FacePoseDefinition;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads FacePose(...) semantics.
 */
final class PoseCallReader {
    private final ArgumentReader arguments = new ArgumentReader();

    AnimationCompileResult read(InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            arguments.requireKnown(call, List.of("rig", "bones", "controls"), diagnostics);
            FacePoseId id = FacePoseId.parse(arguments.string(call, 0, null).orElseThrow());
            FaceRigId rig = FaceRigId.parse(arguments.string(call, -1, "rig").orElseThrow());
            Map<BoneId, Transform3> bones = readBones(call);
            Map<FaceControlId, Double> controls = readControls(call);
            String fingerprint = new SemanticFingerprinter().fingerprint(List.of(id.toString(), rig.toString(),
                    bones.toString(), controls.toString()));
            FacePoseDefinition pose = new FacePoseDefinition(id, rig, bones, controls, fingerprint);
            return new AnimationCompileResult(Optional.empty(), Optional.of(pose), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-POSE", exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private Map<BoneId, Transform3> readBones(InvocationExpressionSyntax call) {
        Map<BoneId, Transform3> result = new LinkedHashMap<>();
        for (InvocationExpressionSyntax delta : arguments.calls(call, "bones")) {
            require(delta, "BoneDelta");
            result.put(BoneId.parse(arguments.string(delta, 0, null).orElseThrow()), transform(delta));
        }
        return result;
    }

    private Map<FaceControlId, Double> readControls(InvocationExpressionSyntax call) {
        Map<FaceControlId, Double> result = new LinkedHashMap<>();
        for (InvocationExpressionSyntax delta : arguments.calls(call, "controls")) {
            require(delta, "ControlDelta");
            FaceControlId id = FaceControlId.parse(arguments.string(delta, 0, null).orElseThrow());
            result.put(id, arguments.authoredNumber(delta, -1, "value", 0));
        }
        return result;
    }

    private Transform3 transform(InvocationExpressionSyntax call) {
        return new Transform3(
                new Vec3(arguments.authoredNumber(call, -1, "x", 0),
                        arguments.authoredNumber(call, -1, "y", 0),
                        arguments.authoredNumber(call, -1, "z", 0)),
                arguments.authoredNumber(call, -1, "rotationZ", 0),
                new Vec3(arguments.authoredNumber(call, -1, "scaleX", 1000),
                        arguments.authoredNumber(call, -1, "scaleY", 1000),
                        arguments.authoredNumber(call, -1, "scaleZ", 1000)),
                Vec3.ZERO);
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Ожидался intrinsic " + name + ".");
        }
    }
}
