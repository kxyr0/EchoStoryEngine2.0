package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.audio.AudioStoryCallBinder;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.BindingContext;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.SemanticDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions.BlackspaceActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character.LinearCharacterActionBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearCallBinding;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearNodeChain;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearStoryIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.observation.ObservationActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.features.FeatureActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep.SleepActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.time.TimeActionIntrinsics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LinearMethodBinder {
    private static final Set<String> VISIBILITY_TARGETS = Set.of("View.Arm", "View.Disarm",
            "View.WaitUntilOutOfView", "View.WaitUntilVisible",
            "View.IsOutOfView", "View.IsVisible", "View.AssertStillOutOfView");
    private final SemanticDiagnosticFactory diagnostics;
    private final SourceNodeFactory nodeFactory = new SourceNodeFactory();
    private final ExternalActionIntrinsics externalActions = new ExternalActionIntrinsics(nodeFactory);
    private final BlackspaceActionIntrinsics blackspaceActions = new BlackspaceActionIntrinsics(nodeFactory);
    private final SleepActionIntrinsics sleepActions = new SleepActionIntrinsics(nodeFactory);
    private final LinearCharacterActionBinder characterActions = new LinearCharacterActionBinder(nodeFactory);
    private final TimeActionIntrinsics timeActions = new TimeActionIntrinsics(nodeFactory);
    private final ObservationActionIntrinsics observationActions = new ObservationActionIntrinsics(nodeFactory);
    private final LinearNodeChain nodeChain = new LinearNodeChain(nodeFactory);
    private final LinearStoryIntrinsics storyActions = new LinearStoryIntrinsics();

    public LinearMethodBinder(SemanticDiagnosticFactory diagnostics) { this.diagnostics = diagnostics; }

    public List<SourceNode> bind(MethodSyntax method, String startNode, BindingContext binding,
            List<StoryDiagnostic> values) {
        if (!validSignature(method)) {
            values.add(diagnostics.create("ESE-SOURCE-025", binding, method.span(), Map.of("method", "OnStart")));
            return List.of();
        }
        return bindBody(new LinearBody(method.body(), method.span(), startNode, binding), values);
    }

    public List<SourceNode> bind(ClassSectionSyntax section, String startNode, BindingContext binding,
            List<StoryDiagnostic> values) {
        return bindBody(new LinearBody(section.body(), section.span(), startNode, binding), values);
    }

    private List<SourceNode> bindBody(LinearBody body, List<StoryDiagnostic> values) {
        List<LinearNodeChain.Entry> pending = pendingNodes(body.body(), body.binding(), values);
        if (!values.isEmpty()) { return List.of(); }
        return nodeChain.link(body.span(), body.startNode(), pending);
    }

    private List<LinearNodeChain.Entry> pendingNodes(BlockStatementSyntax body, BindingContext binding,
            List<StoryDiagnostic> values) {
        List<LinearNodeChain.Entry> pending = new ArrayList<>();
        Map<String, String> blackspaceVariables = new LinkedHashMap<>();
        body.statements().forEach(statement ->
                pending.addAll(bindStatement(statement, binding, values, blackspaceVariables)));
        return pending;
    }

    private List<LinearNodeChain.Entry> bindStatement(StatementSyntax statement, BindingContext binding,
            List<StoryDiagnostic> values, Map<String, String> blackspaceVariables) {
        if (statement instanceof LocalDeclarationStatementSyntax local) {
            return one(bindLocal(local, binding, values, blackspaceVariables));
        }
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax call) {
            return bindCall(call, binding, values, blackspaceVariables);
        }
        values.add(diagnostics.create("ESE-SOURCE-019", binding, statement.span(),
                Map.of("member", "OnStart", "expected", "linear story command")));
        return List.of();
    }

    private Optional<LinearNodeChain.Entry> bindLocal(LocalDeclarationStatementSyntax local, BindingContext binding,
            List<StoryDiagnostic> values,
            Map<String, String> blackspaceVariables) {
        Optional<InvocationExpressionSyntax> call = localCall(local);
        if (call.isEmpty()) { return invalidLocal(local, binding, values); }
        IntrinsicCallReader reader = new IntrinsicCallReader(call.orElseThrow(), diagnostics, binding);
        if (!"Blackspace.Create".equals(reader.targetName())) {
            return invalidLocal(local, binding, values);
        }
        Optional<String> instanceId = blackspaceActions.linearInstanceId(reader);
        Optional<LinearNodeChain.Entry> node = pending("create_blackspace", blackspaceActions.linearCreate(
                reader, local.span(), Optional.of(local.name() + ".Spawn")));
        values.addAll(reader.diagnostics());
        if (reader.hasErrors()) { return Optional.empty(); }
        instanceId.ifPresent(id -> blackspaceVariables.put(local.name(), id));
        return node;
    }

    private Optional<InvocationExpressionSyntax> localCall(LocalDeclarationStatementSyntax local) {
        return local.initializer().filter(InvocationExpressionSyntax.class::isInstance).map(
                InvocationExpressionSyntax.class::cast);
    }

    private Optional<LinearNodeChain.Entry> invalidLocal(LocalDeclarationStatementSyntax local, BindingContext binding,
            List<StoryDiagnostic> values) {
        values.add(diagnostics.create("ESE-SOURCE-019", binding, local.span(),
                Map.of("member", "OnStart", "expected", "blackspace creation local declaration")));
        return Optional.empty();
    }

    private List<LinearNodeChain.Entry> bindCall(InvocationExpressionSyntax call, BindingContext binding,
            List<StoryDiagnostic> values, Map<String, String> blackspaceVariables) {
        if (new AudioCommandBinder().accepts(call)) {
            return new AudioStoryCallBinder(diagnostics).bind(call, binding, values);
        }
        IntrinsicCallReader reader = new IntrinsicCallReader(call, diagnostics, binding);
        LinearCallBinding context = new LinearCallBinding(call, binding, values, blackspaceVariables);
        List<LinearNodeChain.Entry> nodes = FeatureActionIntrinsics.TARGETS.contains(reader.targetName())
                ? one(pending("set_feature", new FeatureActionIntrinsics().linear(reader, call.span())))
                : ObservationActionIntrinsics.TARGETS.contains(reader.targetName())
                ? one(pending(observationActions.id(reader), observationActions.linear(reader, call.span())))
                : bindKnownCall(reader, context);
        values.addAll(reader.diagnostics()); return reader.hasErrors() ? List.of() : nodes;
    }

    private List<LinearNodeChain.Entry> bindKnownCall(IntrinsicCallReader reader, LinearCallBinding context) {
        SourceSpan span = context.call().span();
        if (SleepActionIntrinsics.LINEAR_TARGETS.contains(reader.targetName())) {
            return sleepActions.linearActions(reader, span, context.blackspaceVariables())
                    .stream()
                    .map(action -> new LinearNodeChain.Entry(action.id(), action.spec()))
                    .toList();
        }
        if (VISIBILITY_TARGETS.contains(reader.targetName())) {
            return one(pending("view", externalActions.linearVisibility(reader, span)));
        }
        if (TimeActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return one(pending(timeActions.id(reader), timeActions.linear(reader, span)));
        }
        if (LinearStoryIntrinsics.TARGETS.contains(reader.targetName())) {
            return one(storyActions.bind(reader, span));
        }
        return one(switch (reader.targetName()) {
            case "Screen.SetBlack" -> pending("set_black", externalActions.linearScreenSetBlack(reader, span));
            case "Environment.Set" -> pending("set_environment", externalActions.linearEnvironmentSet(reader, span));
            case "Dialogue.Load" -> pending("load_dialogue", externalActions.linearDialogueLoad(reader, span));
            case "Story.Load" -> pending("load_story", externalActions.linearStoryLoad(reader, span));
            default -> LinearCharacterActionBinder.TARGETS.contains(reader.targetName())
                    ? characterActions.bind(reader, span, context.blackspaceVariables(), externalActions)
                    : unknownIntrinsic(reader, context);
        });
    }

    private Optional<LinearNodeChain.Entry> pending(String id, Optional<NodeSpec> spec) {
        return spec.map(value -> new LinearNodeChain.Entry(id, value));
    }

    private List<LinearNodeChain.Entry> one(Optional<LinearNodeChain.Entry> node) {
        return node.map(List::of).orElseGet(List::of);
    }

    private Optional<LinearNodeChain.Entry> unknownIntrinsic(IntrinsicCallReader reader, LinearCallBinding context) {
        context.values().add(diagnostics.create("ESE-SOURCE-021", context.binding(), context.call().span(),
                Map.of("intrinsic", reader.targetName())));
        return Optional.empty();
    }

    private boolean validSignature(MethodSyntax method) {
        return method.name().equals("OnStart") && method.returnType().isVoid() && method.parameters().isEmpty()
                && method.modifiers().contains(SyntaxModifier.PRIVATE);
    }

    private record LinearBody(
            BlockStatementSyntax body,
            SourceSpan span,
            String startNode,
            BindingContext binding) {
    }

}
