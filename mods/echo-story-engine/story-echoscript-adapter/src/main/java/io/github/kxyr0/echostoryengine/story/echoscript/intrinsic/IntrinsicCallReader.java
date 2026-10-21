package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.value.IntrinsicReferenceReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.value.IntrinsicValueReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class IntrinsicCallReader {
    private final InvocationExpressionSyntax call;
    private final SemanticDiagnosticFactory diagnostics;
    private final BindingContext binding;
    private final List<StoryDiagnostic> values = new ArrayList<>();
    private final IntrinsicValueReader valueReader = new IntrinsicValueReader(this::report);

    public IntrinsicCallReader(InvocationExpressionSyntax call, SemanticDiagnosticFactory diagnostics,
                               BindingContext binding) {
        this.call = call;
        this.diagnostics = diagnostics;
        this.binding = binding;
    }

    public String targetName() {
        if (call.target() instanceof NameExpressionSyntax name) { return name.name(); }
        if (call.target() instanceof MemberAccessExpressionSyntax access
                && access.target() instanceof NameExpressionSyntax qualifier) {
            return qualifier.name() + "." + access.memberName();
        }
        return "";
    }

    public void requirePositionalCount(String intrinsic, int count) {
        int actual = positionals().size();
        if (actual != count) {
            report("ESE-SOURCE-022", Map.of(
                    "intrinsic", intrinsic,
                    "expected", Integer.toString(count),
                    "actual", Integer.toString(actual)));
        }
    }

    public void requirePositionalCount(String intrinsic, Set<Integer> counts) {
        int actual = positionals().size();
        if (!counts.contains(actual)) {
            report("ESE-SOURCE-022", Map.of(
                    "intrinsic", intrinsic,
                    "expected", counts.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("/")),
                    "actual", Integer.toString(actual)));
        }
    }

    public void rejectUnknownNamed(String intrinsic, Set<String> allowed) {
        call.arguments().stream()
                .flatMap(argument -> argument.name().stream())
                .filter(name -> !allowed.contains(name))
                .forEach(name -> report("ESE-SOURCE-023", Map.of("intrinsic", intrinsic, "argument", name)));
    }

    public Optional<String> positionalString(int index, String intrinsic, String argument) {
        if (positionals().size() <= index) { return Optional.empty(); }
        return valueReader.string(positionals().get(index).value(), intrinsic, argument);
    }

    public Optional<String> positionalScalar(int index, String intrinsic, String argument) {
        if (positionals().size() <= index) { return Optional.empty(); }
        return valueReader.scalar(positionals().get(index).value(), intrinsic, argument);
    }

    public Optional<List<String>> positionalVector(int index, String intrinsic, String argument) {
        return positionals().size() <= index ? Optional.empty()
                : valueReader.vector(positionals().get(index).value(), intrinsic, argument); }

    public Optional<String> positionalReference(int index, String intrinsic, String argument) {
        if (positionals().size() <= index) {
            return Optional.empty();
        }
        Optional<String> value = IntrinsicReferenceReader.read(positionals().get(index).value());
        if (value.isEmpty()) {
            report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", argument,
                    "expected", "literal-or-reference"));
        }
        return value;
    }

    public Optional<String> positionalReferenceOrVector(int index, String intrinsic, String argument) {
        if (positionals().size() <= index) { return Optional.empty(); }
        ExpressionSyntax expression = positionals().get(index).value();
        Optional<String> reference = IntrinsicReferenceReader.read(expression);
        if (reference.isPresent()) { return reference; }
        Optional<List<String>> vector = valueReader.vector(expression, intrinsic, argument);
        return vector.map(values -> "Position(" + String.join(",", values) + ")");
    }

    public Optional<UnaryValue> positionalInvocationScalar(
            int index, String intrinsic, String argument, Set<String> targets) {
        if (positionals().size() <= index) { return Optional.empty(); }
        ExpressionSyntax expression = positionals().get(index).value();
        if (expression instanceof InvocationExpressionSyntax invocation
                && invocation.target() instanceof NameExpressionSyntax name
                && targets.contains(name.name())
                && invocation.arguments().size() == 1
                && invocation.arguments().get(0).name().isEmpty()) {
            return valueReader.scalar(invocation.arguments().get(0).value(), intrinsic, argument)
                    .map(value -> new UnaryValue(name.name(), value));
        }
        report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic, "argument", argument,
                "expected", String.join("/", targets) + "(literal)"));
        return Optional.empty();
    }

    public int positionalCount() { return positionals().size(); }

    public Optional<String> requiredNamedString(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            report("ESE-SOURCE-022", Map.of("intrinsic", intrinsic, "missing", name));
            return Optional.empty();
        }
        return valueReader.string(argument.orElseThrow().value(), intrinsic, name);
    }

    public Optional<String> optionalNamedString(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            return Optional.empty();
        }
        return valueReader.string(argument.orElseThrow().value(), intrinsic, name);
    }

    public Optional<String> optionalNamedScalar(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            return Optional.empty();
        }
        return valueReader.scalar(argument.orElseThrow().value(), intrinsic, name);
    }

    public Optional<String> optionalNamedReference(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) { return Optional.empty(); }
        Optional<String> value = IntrinsicReferenceReader.read(argument.orElseThrow().value());
        if (value.isEmpty()) { report("ESE-SOURCE-024", Map.of("intrinsic", intrinsic,
                "argument", name, "expected", "literal-or-reference")); }
        return value;
    }

    public Optional<String> requiredNamedReference(String name, String intrinsic) {
        Optional<String> value = optionalNamedReference(name, intrinsic);
        if (named(name).isEmpty()) {
            report("ESE-SOURCE-022", Map.of("intrinsic", intrinsic, "missing", name));
        }
        return value;
    }

    public Optional<String> requiredNamedScalar(String name, String intrinsic) {
        Optional<ArgumentSyntax> argument = named(name);
        if (argument.isEmpty()) {
            report("ESE-SOURCE-022", Map.of("intrinsic", intrinsic, "missing", name));
            return Optional.empty();
        }
        return valueReader.scalar(argument.orElseThrow().value(), intrinsic, name);
    }

    public Map<String, String> additionalNamedScalars(String intrinsic, Set<String> reserved) {
        Map<String, String> values = new LinkedHashMap<>();
        call.arguments().stream()
                .filter(argument -> argument.name().filter(name -> !reserved.contains(name)).isPresent())
                .forEach(argument -> appendScalar(values, argument, intrinsic));
        return Collections.unmodifiableMap(values);
    }

    public List<StoryDiagnostic> diagnostics() { return List.copyOf(values); }

    public boolean hasErrors() { return !values.isEmpty(); }

    private void appendScalar(Map<String, String> values, ArgumentSyntax argument, String intrinsic) {
        String name = argument.name().orElseThrow();
        valueReader.scalar(argument.value(), intrinsic, name).ifPresent(value -> values.put(name, value));
    }

    private Optional<ArgumentSyntax> named(String name) {
        return call.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst();
    }

    private List<ArgumentSyntax> positionals() {
        return call.arguments().stream().filter(argument -> argument.name().isEmpty()).toList();
    }

    public void report(String code, Map<String, String> context) {
        values.add(diagnostics.create(code, binding, call.span(), context));
    }

    public record UnaryValue(String function, String value) { }
}
