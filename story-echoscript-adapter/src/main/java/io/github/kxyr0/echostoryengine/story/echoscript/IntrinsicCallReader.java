package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads allowlisted intrinsic arguments from one EchoScript invocation.
 */
final class IntrinsicCallReader {
    private final InvocationExpressionSyntax call;
    private final SemanticDiagnosticFactory diagnostics;
    private final BindingContext binding;
    private final List<StoryDiagnostic> values = new ArrayList<>();

    IntrinsicCallReader(
            InvocationExpressionSyntax call,
            SemanticDiagnosticFactory diagnostics,
            BindingContext binding) {
        this.call = call;
        this.diagnostics = diagnostics;
        this.binding = binding;
    }

    String targetName() {
        return call.target() instanceof NameExpressionSyntax name ? name.name() : "";
    }

    void requirePositionalCount(String intrinsic, int count) {
        int actual = positionals().size();
        if (actual != count) {
            report("ESE-SOURCE-022", Map.of(
                    "intrinsic", intrinsic,
                    "expected", Integer.toString(count),
                    "actual", Integer.toString(actual)));
        }
    }

    void rejectUnknownNamed(String intrinsic, Set<String> allowed) {
        call.arguments().stream()
                .flatMap(argument -> argument.name().stream())
                .filter(name -> !allowed.contains(name))
                .forEach(name -> report("ESE-SOURCE-023", Map.of("intrinsic", intrinsic, "argument", name)));
    }

    Optional<String> positionalString(int index, String intrinsic, String argument) {
        if (positionals().size() <= index) {
            return Optional.empty();
        }
        return stringValue(positionals().get(index).value(), intrinsic, argument);
    }

    Optional<String> requiredNamedString(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            report("ESE-SOURCE-022", Map.of("intrinsic", intrinsic, "missing", name));
            return Optional.empty();
        }
        return stringValue(argument.orElseThrow().value(), intrinsic, name);
    }

    Optional<String> requiredNamedScalar(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            report("ESE-SOURCE-022", Map.of("intrinsic", intrinsic, "missing", name));
            return Optional.empty();
        }
        return scalarValue(argument.orElseThrow().value(), intrinsic, name);
    }

    List<StoryDiagnostic> diagnostics() {
        return List.copyOf(values);
    }

    boolean hasErrors() {
        return !values.isEmpty();
    }

    private Optional<String> stringValue(ExpressionSyntax expression, String intrinsic, String argument) {
        if (expression instanceof StringLiteralExpressionSyntax value) {
            return Optional.of(value.value());
        }
        report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", argument, "expected", "string"));
        return Optional.empty();
    }

    private Optional<String> scalarValue(ExpressionSyntax expression, String intrinsic, String argument) {
        if (expression instanceof StringLiteralExpressionSyntax value) {
            return Optional.of(value.value());
        }
        if (expression instanceof BooleanLiteralExpressionSyntax value) {
            return Optional.of(Boolean.toString(value.value()));
        }
        if (expression instanceof IntegerLiteralExpressionSyntax value) {
            return Optional.of(Integer.toString(value.value()));
        }
        report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", argument, "expected", "literal"));
        return Optional.empty();
    }

    private Optional<ArgumentSyntax> named(String name) {
        return call.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst();
    }

    private List<ArgumentSyntax> positionals() {
        return call.arguments().stream().filter(argument -> argument.name().isEmpty()).toList();
    }

    private void report(String code, Map<String, String> context) {
        values.add(diagnostics.create(code, binding, call.span(), context));
    }
}
