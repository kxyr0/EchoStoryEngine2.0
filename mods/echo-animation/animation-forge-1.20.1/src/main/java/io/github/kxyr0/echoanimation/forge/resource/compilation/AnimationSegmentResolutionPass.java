package io.github.kxyr0.echoanimation.forge.resource.compilation;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.definition.resolve.AnimationDefinitionSegmentResolver;
import java.util.List;

/**
 * Links procedural references once all independently compiled resources are available.
 */
public final class AnimationSegmentResolutionPass {
    public void resolve(List<AnimationClipDefinition> clips, List<AnimationDefinition> definitions,
            List<AnimationDiagnostic> diagnostics) {
        AnimationDefinitionSegmentResolver resolver = new AnimationDefinitionSegmentResolver();
        for (int index = 0; index < definitions.size(); index++) {
            try {
                definitions.set(index, resolver.resolve(definitions.get(index), clips));
            } catch (IllegalArgumentException exception) {
                diagnostics.add(new AnimationDiagnostic("EANIM-FORGE-SEGMENT", DiagnosticSeverity.ERROR,
                        exception.getMessage()));
            }
        }
    }
}
