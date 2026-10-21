package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character.NodeCharacterActionBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.observation.ObservationActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep.SleepActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.time.TimeActionIntrinsics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Binds the required Nodes method into neutral source nodes.
public final class NodeMethodBinder {
    private static final Set<String> EXTERNAL_TARGETS = externalTargets();
    private final SemanticDiagnosticFactory diagnostics;
    private final SourceNodeFactory nodeFactory = new SourceNodeFactory();
    private final ExternalActionIntrinsics externalActions = new ExternalActionIntrinsics(nodeFactory);
    private final SleepActionIntrinsics sleepActions = new SleepActionIntrinsics(nodeFactory);
    private final NodeCharacterActionBinder characterActions = new NodeCharacterActionBinder(nodeFactory);
    private final TimeActionIntrinsics timeActions = new TimeActionIntrinsics(nodeFactory);
    private final ObservationActionIntrinsics observationActions = new ObservationActionIntrinsics(nodeFactory);

    public NodeMethodBinder(SemanticDiagnosticFactory diagnostics) { this.diagnostics = diagnostics; }

    public List<SourceNode> bind(MethodSyntax method, BindingContext binding, List<StoryDiagnostic> values) {
        if (!validSignature(method)) {
            values.add(diagnostics.create("ESE-SOURCE-025", binding, method.span(), Map.of("method", "Nodes")));
            return List.of();
        }
        List<SourceNode> nodes = new ArrayList<>();
        for (StatementSyntax statement : method.body().statements()) {
            bindStatement(statement, binding, values).ifPresent(nodes::add);
        }
        return List.copyOf(nodes);
    }

    private Optional<SourceNode> bindStatement(StatementSyntax statement, BindingContext binding,
            List<StoryDiagnostic> values) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax call) {
            return bindCall(call, binding, values);
        }
        values.add(diagnostics.create("ESE-SOURCE-019", binding, statement.span(),
                Map.of("member", "Nodes", "expected", "node intrinsic call")));
        return Optional.empty();
    }

    private Optional<SourceNode> bindCall(InvocationExpressionSyntax call, BindingContext binding,
            List<StoryDiagnostic> values) {
        IntrinsicCallReader reader = new IntrinsicCallReader(call, diagnostics, binding);
        Optional<SourceNode> node = bindExternalCall(reader, call.span());
        if (node.isEmpty() && !EXTERNAL_TARGETS.contains(reader.targetName())) {
            node = bindCoreCall(reader, call, binding, values);
        }
        values.addAll(reader.diagnostics()); return reader.hasErrors() ? Optional.empty() : node;
    }

    private Optional<SourceNode> bindCoreCall(IntrinsicCallReader reader, InvocationExpressionSyntax call,
            BindingContext binding, List<StoryDiagnostic> values) {
        return switch (reader.targetName()) {
            case "Start" -> start(reader, call.span());
            case "Set" -> set(reader, call.span());
            case "Require" -> require(reader, call.span());
            case "Action" -> action(reader, call.span());
            case "Finish" -> finish(reader, call.span());
            default -> NodeCharacterActionBinder.TARGETS.contains(reader.targetName())
                    ? characterActions.bind(reader, call.span())
                    : unknownIntrinsic(reader, call, binding, values);
        };
    }

    private Optional<SourceNode> bindExternalCall(IntrinsicCallReader reader, SourceSpan span) {
        if (SleepActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return sleepActions.node(reader, span);
        }
        if (TimeActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return timeActions.node(reader, span);
        }
        if (ObservationActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return observationActions.node(reader, span);
        }
        return switch (reader.targetName()) {
            case "Dialogue.Load" -> externalActions.dialogueLoad(reader, span);
            case "Story.Load" -> externalActions.storyLoad(reader, span);
            case "Environment.Set" -> externalActions.environmentSet(reader, span);
            default -> Optional.empty();
        };
    }

    private static Set<String> externalTargets() {
        java.util.LinkedHashSet<String> targets = new java.util.LinkedHashSet<>(Set.of(
                "Dialogue.Load", "Story.Load", "Environment.Set"));
        targets.addAll(SleepActionIntrinsics.TARGETS);
        targets.addAll(TimeActionIntrinsics.TARGETS);
        targets.addAll(ObservationActionIntrinsics.TARGETS);
        return Set.copyOf(targets);
    }

    private Optional<SourceNode> start(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Start", 1);
        reader.rejectUnknownNamed("Start", Set.of("next"));
        return nodeFactory.node(reader, NodeSpec.start(span, reader.requiredNamedString("next", "Start")));
    }

    private Optional<SourceNode> set(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Set", 1);
        reader.rejectUnknownNamed("Set", Set.of("variable", "value", "next"));
        return nodeFactory.node(reader, NodeSpec.set(
                span,
                reader.requiredNamedString("next", "Set"),
                reader.requiredNamedString("variable", "Set"),
                reader.requiredNamedScalar("value", "Set")));
    }

    private Optional<SourceNode> require(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Require", 1);
        reader.rejectUnknownNamed("Require", Set.of("variable", "value", "next"));
        return nodeFactory.node(reader, NodeSpec.require(
                span,
                reader.requiredNamedString("next", "Require"),
                reader.requiredNamedString("variable", "Require"),
                reader.requiredNamedScalar("value", "Require")));
    }

    private Optional<SourceNode> action(IntrinsicCallReader reader, SourceSpan span) {
        Set<String> reserved = Set.of("type", "output", "next");
        reader.requirePositionalCount("Action", 1);
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Action"),
                reader.requiredNamedString("type", "Action"),
                reader.optionalNamedString("output", "Action"),
                reader.additionalNamedScalars("Action", reserved))));
    }

    private Optional<SourceNode> finish(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Finish", 1);
        reader.rejectUnknownNamed("Finish", Set.of());
        return reader.positionalString(0, "Finish", "id").map(id -> nodeFactory.sourceNode(id, NodeSpec.finish(span)));
    }

    private Optional<SourceNode> unknownIntrinsic(IntrinsicCallReader reader, InvocationExpressionSyntax call,
            BindingContext binding, List<StoryDiagnostic> values) {
        values.add(diagnostics.create("ESE-SOURCE-021", binding, call.span(),
                Map.of("intrinsic", reader.targetName())));
        return Optional.empty();
    }

    private boolean validSignature(MethodSyntax method) {
        return method.name().equals("Nodes") && method.returnType().isVoid()
                && method.parameters().isEmpty() && method.modifiers().contains(SyntaxModifier.PRIVATE);
    }
}
