package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Entry point for high-level face rig resources.
 */
public final class FaceScriptCompiler {
    private static final Pattern START = Pattern.compile(
            "\\A\\s*(namespace\\s+[A-Za-z_][A-Za-z0-9_.]*\\s*;\\s*)?face\\s+");

    private final FaceScriptParser parser = new FaceScriptParser();
    private final FaceRigLowerer lowerer = new FaceRigLowerer();

    public Optional<AnimationCompileResult> compile(String sourceText) {
        if (!START.matcher(sourceText).find()) {
            return Optional.empty();
        }
        try {
            FaceScriptModel.Face face = parser.parse(sourceText);
            FaceRigDefinition rig = lowerer.lower(face);
            return Optional.of(new AnimationCompileResult(Optional.of(rig), Optional.empty(), Optional.empty(),
                    List.of()));
        } catch (RuntimeException exception) {
            AnimationDiagnostic diagnostic = AnimationSemanticDiagnostics.error("EANIM-FACE-SCRIPT",
                    exception.getMessage());
            return Optional.of(new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    List.of(diagnostic)));
        }
    }
}
