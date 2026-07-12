package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Binds the required StoryResource Build method.
 */
final class BuildMethodBinder {
    private final SemanticDiagnosticFactory diagnostics;

    BuildMethodBinder(SemanticDiagnosticFactory diagnostics) {
        this.diagnostics = diagnostics;
    }

    Optional<StoryHeader> bind(
            MethodSyntax method,
            BindingContext binding,
            List<StoryDiagnostic> values) {
        if (!validSignature(method)) {
            values.add(diagnostics.create("ESE-SOURCE-018", binding, method.span(), Map.of("method", "Build")));
            return Optional.empty();
        }
        Optional<InvocationExpressionSyntax> storyCall = storyReturn(method);
        if (storyCall.isEmpty()) {
            values.add(diagnostics.create("ESE-SOURCE-018", binding, method.span(),
                    Map.of("method", "Build", "expected", "return Story(...)")));
            return Optional.empty();
        }
        return bindStoryCall(storyCall.orElseThrow(), binding, values);
    }

    private Optional<StoryHeader> bindStoryCall(
            InvocationExpressionSyntax call,
            BindingContext binding,
            List<StoryDiagnostic> values) {
        IntrinsicCallReader reader = new IntrinsicCallReader(call, diagnostics, binding);
        if (!"Story".equals(reader.targetName())) {
            values.add(diagnostics.create("ESE-SOURCE-021", binding, call.span(),
                    Map.of("intrinsic", reader.targetName())));
            return Optional.empty();
        }
        reader.requirePositionalCount("Story", 1);
        reader.rejectUnknownNamed("Story", Set.of("start"));
        Optional<String> storyId = reader.positionalString(0, "Story", "storyId");
        Optional<String> start = reader.requiredNamedString("start", "Story");
        values.addAll(reader.diagnostics());
        if (reader.hasErrors() || storyId.isEmpty() || start.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StoryHeader(storyId.orElseThrow(), start.orElseThrow()));
    }

    private boolean validSignature(MethodSyntax method) {
        return method.name().equals("Build")
                && method.returnType().arrayRank() == 0
                && method.returnType().name().text().equals("StoryResourceDef")
                && method.parameters().isEmpty()
                && method.modifiers().containsAll(List.of(SyntaxModifier.PUBLIC, SyntaxModifier.OVERRIDE));
    }

    private Optional<InvocationExpressionSyntax> storyReturn(MethodSyntax method) {
        List<StatementSyntax> statements = new ArrayList<>(method.body().statements());
        if (statements.size() != 1 || !(statements.get(0) instanceof ReturnStatementSyntax statement)) {
            return Optional.empty();
        }
        return statement.value().filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }
}
