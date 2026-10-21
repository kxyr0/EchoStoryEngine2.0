package io.github.kxyr0.echoanimation.echoscript.body;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationAttachmentSocketDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.definition.validation.AnimationRigValidator;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads AnimationRig(...) body resource semantics.
 */
public final class BodyRigCallReader {
    private final ArgumentReader arguments = new ArgumentReader();

    public AnimationCompileResult read(InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            arguments.requireKnown(call, List.of("kind", "skin", "bones", "sockets"), diagnostics);
            AnimationRigId id = AnimationRigId.parse(arguments.string(call, 0, null).orElseThrow());
            AnimationRigKind kind = AnimationRigKind.valueOf(arguments.string(call, -1, "kind")
                    .orElse("SEGMENTED_HUMANOID"));
            AnimationSkinVariant skin = AnimationSkinVariant.valueOf(arguments.string(call, -1, "skin")
                    .orElse("CLASSIC"));
            List<AnimationBoneDefinition> bones = bones(call, kind);
            List<AnimationAttachmentSocketDefinition> sockets = sockets(call);
            String fingerprint = fingerprint(id, kind, skin, bones, sockets);
            AnimationRigDefinition rig = new AnimationRigDefinition(id, 1L, kind, skin, bones, List.of(),
                    sockets, fingerprint);
            new AnimationRigValidator().validate(rig);
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.of(rig), Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-BODY-RIG",
                    exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private String fingerprint(
            AnimationRigId id,
            AnimationRigKind kind,
            AnimationSkinVariant skin,
            List<AnimationBoneDefinition> bones,
            List<AnimationAttachmentSocketDefinition> sockets
    ) {
        return new SemanticFingerprinter().fingerprint(List.of(id.toString(), kind.name(), skin.name(),
                bones.toString(), sockets.toString()));
    }

    private List<AnimationBoneDefinition> bones(InvocationExpressionSyntax call, AnimationRigKind kind) {
        List<InvocationExpressionSyntax> calls = arguments.calls(call, "bones");
        if (calls.isEmpty() && kind == AnimationRigKind.SEGMENTED_HUMANOID) {
            return SegmentedHumanoidBone.defaultDefinitions();
        }
        return calls.stream().map(this::bone).toList();
    }

    private AnimationBoneDefinition bone(InvocationExpressionSyntax call) {
        require(call, "Bone");
        BoneId id = BoneId.parse(arguments.string(call, 0, null).orElseThrow());
        Optional<BoneId> parent = arguments.string(call, -1, "parent").map(BoneId::parse);
        return new AnimationBoneDefinition(id, parent, transform(call), Optional.empty());
    }

    private List<AnimationAttachmentSocketDefinition> sockets(InvocationExpressionSyntax call) {
        List<InvocationExpressionSyntax> calls = arguments.calls(call, "sockets");
        if (calls.isEmpty()) {
            return List.of(socket("main_hand", SegmentedHumanoidBone.RIGHT_FOREARM),
                    socket("off_hand", SegmentedHumanoidBone.LEFT_FOREARM));
        }
        return calls.stream().map(this::socket).toList();
    }

    private AnimationAttachmentSocketDefinition socket(InvocationExpressionSyntax call) {
        require(call, "Socket");
        BoneId parent = BoneId.parse(arguments.string(call, -1, "bone").orElseThrow());
        return new AnimationAttachmentSocketDefinition(
                AnimationAttachmentSocketId.parse(arguments.string(call, 0, null).orElseThrow()),
                parent,
                transform(call));
    }

    private AnimationAttachmentSocketDefinition socket(String id, SegmentedHumanoidBone parent) {
        EulerTransform local = EulerTransform.IDENTITY.withTranslation(new Vec3(0.0D, -6.0D, 0.0D));
        return new AnimationAttachmentSocketDefinition(AnimationAttachmentSocketId.parse(id), parent.id(), local);
    }

    private EulerTransform transform(InvocationExpressionSyntax call) {
        return new EulerTransform(vector(call, ""), vector(call, "rot"),
                new Vec3(arguments.authoredNumber(call, -1, "scaleX", 1000),
                        arguments.authoredNumber(call, -1, "scaleY", 1000),
                        arguments.authoredNumber(call, -1, "scaleZ", 1000)),
                vector(call, "pivot"));
    }

    private Vec3 vector(InvocationExpressionSyntax call, String prefix) {
        return new Vec3(arguments.authoredNumber(call, -1, field(prefix, "X"), 0),
                arguments.authoredNumber(call, -1, field(prefix, "Y"), 0),
                arguments.authoredNumber(call, -1, field(prefix, "Z"), 0));
    }

    private String field(String prefix, String axis) {
        if (prefix.isEmpty()) {
            return axis.toLowerCase(Locale.ROOT);
        }
        return prefix + axis;
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Expected intrinsic " + name + ".");
        }
    }
}
