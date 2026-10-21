package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.AliasTable;
import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.backpack.BackpackDialogueCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.NodeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NodeContentReader {
    private final NodeCommandReader commandReader = new NodeCommandReader();
    private final List<NodeCallReader> callReaders;

    public NodeContentReader(List<DialogueLineArgumentBinder> extensions) {
        callReaders = List.of(
                new DialogueNodeCallReader(new NodeLineReader(extensions), commandReader),
                new NpcNodeCallReader(commandReader, new BackpackDialogueCommandReader()),
                new VisualNodeCallReader(commandReader),
                new ExternalNodeCallReader(commandReader));
    }

    public Optional<CompiledDialogueNode> readNode(
            DialogueNodeId nodeId,
            List<StatementSyntax> statements,
            ReadContext context) {
        NodeReadState state = new NodeReadState(nodeId, context);

        for (StatementSyntax statement : statements) {
            CallSyntax.statementCall(statement)
                    .ifPresent(call -> readCall(state, call));
        }

        List<CompiledDialogueCommand> commands =
                commandReader.valid(
                        state.commands,
                        state.lines.size(),
                        context.diagnostics());

        boolean authoredEmpty = state.isEmpty();
        if (context.autoComplete() && !state.isTerminalDeclared()) {
            state.completion = Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.DEFAULT));
        }

        if (authoredEmpty) {
            context.diagnostics().add(
                    context.autoComplete()
                            ? DialogueSemanticDiagnostics.warning("ECHO_SCRIPT_EMPTY_DIALOGUE_NODE",
                                    "Empty dialogue node completes immediately.")
                            : DialogueSemanticDiagnostics.semantic("EDLG-SEM-018",
                                    "Node must contain Line, Choice, Complete or Dialogue transition."));
        }

        return Optional.of(
                new CompiledDialogueNode(
                        nodeId,
                        state.lines,
                        state.choices,
                        commands,
                        state.completion,
                        state.transition));
    }

    private void readCall(
            NodeReadState state,
            InvocationExpressionSyntax call) {
        String rootName = CallSyntax.rootName(call);

        for (NodeCallReader reader : callReaders) {
            if (reader.read(rootName, state, call)) {
                return;
            }
        }

        state.context.diagnostics().add(
                DialogueSemanticDiagnostics.unsupported(
                        CallSyntax.qualifiedName(call),
                        call));
    }

    static final class NodeReadState {
        final DialogueNodeId nodeId;
        final ReadContext context;
        final List<CompiledDialogueLine> lines = new ArrayList<>();
        final List<CompiledDialogueChoice> choices = new ArrayList<>();
        final List<CompiledDialogueCommand> commands = new ArrayList<>();
        Optional<CompiledDialogueCompletion> completion = Optional.empty();
        Optional<CompiledDialogueNode.Transition> transition = Optional.empty();

        NodeReadState(
                DialogueNodeId nodeId,
                ReadContext context) {
            this.nodeId = nodeId;
            this.context = context;
        }

        boolean isTerminalDeclared() {
            return !choices.isEmpty()
                    || completion.isPresent()
                    || transition.isPresent();
        }

        boolean isEmpty() {
            return lines.isEmpty()
                    && choices.isEmpty()
                    && completion.isEmpty()
                    && transition.isEmpty();
        }
    }

    public record ReadContext(
            AliasTable aliases,
            MethodCatalog methods,
            Map<String, DialogueNodeId> methodNodeIds,
            List<DialogueDiagnostic> diagnostics,
            Optional<String> implicitSpeakerNamespace,
            boolean autoComplete) {

        NodeLineReader.LineContext lineContext() {
            return new NodeLineReader.LineContext(
                    aliases,
                    methods,
                    diagnostics,
                    implicitSpeakerNamespace);
        }
    }
}
