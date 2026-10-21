package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear;

import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import java.util.List;
import java.util.Map;

public record LinearCallBinding(
        InvocationExpressionSyntax call,
        BindingContext binding,
        List<StoryDiagnostic> values,
        Map<String, String> blackspaceVariables) {
}
