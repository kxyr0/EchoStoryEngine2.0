package io.github.kxyr0.echoanimation.echoscript.body;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;

import java.util.List;
import java.util.Optional;

/** Reads AnimationClip(...) normalized body clip semantics. */
public final class BodyClipCallReader {
    private final ArgumentReader arguments = new ArgumentReader();
    private final BodyClipFrameReader frames = new BodyClipFrameReader();

    public AnimationCompileResult read( InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        try {
            return parse(call, diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add( AnimationSemanticDiagnostics.error(
                            "EANIM-ES-BODY-CLIP", exception.getMessage(), call.span()));
            return new AnimationCompileResult( Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private AnimationCompileResult parse( InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        arguments.requireKnown( call, List.of( "rig", "tracks", "schemaVersion", "loopSeam",
                        "authoredSpeedBlocksPerSecond"), diagnostics);
        AnimationClipId id = AnimationClipId.parse(arguments.string(call, 0, null).orElseThrow());
        Optional<AnimationRigId> rig = arguments.string(call, -1, "rig").map(AnimationRigId::parse);
        List<AnimationBoneTrackDefinition> tracks = arguments.calls(call, "tracks").stream().map(this::track).toList();
        int schemaVersion = arguments.integer(call, -1, "schemaVersion", 1);
        AnimationLoopSeam loopSeam = arguments .string(call, -1, "loopSeam") .map(AnimationLoopSeam::valueOf)
                        .orElse(AnimationLoopSeam.HOLD);
        double authoredSpeed = arguments.rawNumber(call, -1, "authoredSpeedBlocksPerSecond", 1.0D);
        String fingerprint = new SemanticFingerprinter() .fingerprint( List.of( id.toString(), rig.toString(),
                                        tracks.toString(), Integer.toString(schemaVersion), loopSeam.name(),
                                        Double.toString(authoredSpeed)));
        AnimationClipDefinition clip = AnimationClipDefinition.legacy( id, rig, tracks,
                        new AnimationClipDefinition.LegacySettings(
                                schemaVersion, loopSeam, authoredSpeed, fingerprint));
        return new AnimationCompileResult( Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(clip), Optional.empty(), Optional.empty(), diagnostics);
    }

    private AnimationBoneTrackDefinition track(InvocationExpressionSyntax call) {
        require(call, "Track");
        BoneId id = BoneId.parse(arguments.string(call, 0, null).orElseThrow());
        List<AnimationKeyframeDefinition> frames =
                arguments.calls(call, "frames").stream().map(this.frames::read).toList();
        return new AnimationBoneTrackDefinition(id, frames);
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Expected intrinsic " + name + ".");
        }
    }
}
