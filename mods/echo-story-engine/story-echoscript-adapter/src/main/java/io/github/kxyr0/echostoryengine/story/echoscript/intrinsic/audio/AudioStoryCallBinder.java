package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.audio;

import io.github.kxyr0.echoaudio.api.AudioFailure;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearNodeChain;
import java.util.List;
import java.util.Map;

public final class AudioStoryCallBinder {
    private final SemanticDiagnosticFactory diagnostics;
    public AudioStoryCallBinder(SemanticDiagnosticFactory diagnostics) { this.diagnostics = diagnostics; }
    public List<LinearNodeChain.Entry> bind(InvocationExpressionSyntax call, BindingContext binding,
            List<StoryDiagnostic> values) {
        try {
            return new AudioActionIntrinsics().bind(call).map(List::of).orElseGet(List::of);
        } catch (AudioFailure failure) {
            StoryDiagnostic context = diagnostics.create("ESE-SOURCE-024", binding, call.span(),
                    Map.of("intrinsic", "Sound", "argument", "audio", "expected", failure.getMessage()));
            values.add(new StoryDiagnostic(failure.code(), context.severity(), context.subsystem(),
                    failure.getMessage(), context.subject(), context.context(), context.causeCategory(),
                    context.occurredAt(), context.operatorHint()));
            return List.of();
        }
    }
}
