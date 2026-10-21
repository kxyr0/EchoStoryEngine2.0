package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;
import io.github.kxyr0.echodialogue.echoscript.binding.node.NodeContentReader.NodeReadState;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.echoscript.binding.command.NodeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class DialogueNodeCallReader implements NodeCallReader {
    private final NodeLineReader lineReader;
    private final NodeCommandReader commandReader;

    DialogueNodeCallReader(NodeLineReader lineReader,
            NodeCommandReader commandReader) {
        this.lineReader = lineReader; this.commandReader = commandReader;
    }

    @Override
    public boolean read(String rootName,
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        switch (rootName) {
            case "Line" -> readLine(state, call);
            case "LineAfterMove" -> readLineAfterMove(state, call);
            case "Delay" -> readDelayCommand(state, call);
            case "Choice" -> readChoice(state, call);
            case "Complete" -> readComplete(state, call);
            case "Dialogue.GoTo", "GoTo" -> readGoTo(state, call);
            case "Dialogue.Load" -> readLoad(state, call);
            case "NodeId" -> { }
            default -> {
                return false;
            }
        }

        return true;
    }

    private void readLine(NodeReadState state, InvocationExpressionSyntax call) {
        if (state.isTerminalDeclared()) {
            diagnose(state, "EDLG-SEM-019", "Line cannot follow Choice, Complete or transition.", call);
            return;
        }
        lineReader.readLine(state.nodeId, state.lines.size(), call, state.context.lineContext())
                .ifPresent(state.lines::add);
    }

    private void readLineAfterMove(NodeReadState state, InvocationExpressionSyntax call) {
        if (state.isTerminalDeclared()) {
            diagnose(state, "EDLG-SEM-019", "LineAfterMove cannot follow a terminal declaration.", call);
            return;
        }
        commandReader.moveArrival(call, state.lines.size(), actorResolver(state), state.context.diagnostics())
                .ifPresent(state.commands::add);
        lineReader.readLine(state.nodeId, state.lines.size(), call, state.context.lineContext())
                .ifPresent(state.lines::add);
    }

    private void readDelayCommand(NodeReadState state, InvocationExpressionSyntax call) {
        if (state.isTerminalDeclared()) {
            diagnose(state, "EDLG-SEM-073", "Delay cannot follow Choice, Complete or transition.", call);
            return;
        }
        commandReader.delay(call, state.lines.size(), state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readChoice(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (state.completion.isPresent() || state.transition.isPresent()) {
            diagnose(state, "EDLG-SEM-020", "Choice cannot be declared after Complete or transition.", call);
            return;
        }
        Optional<String> id = CallSyntax.positionalString(call, 0);
        Optional<String> text = CallSyntax.positionalString(call, 1);
        Optional<DialogueNodeId> next = choiceTarget(call, state);
        if (id.isEmpty() || text.isEmpty() || next.isEmpty()) {
            diagnose(state, "EDLG-SEM-021", "Choice requires id, text and target node.", call);
            return;
        }
        try {
            state.choices.add(new CompiledDialogueChoice(new ChoiceId(id.orElseThrow()), text.orElseThrow(),
                    next.orElseThrow()));
        } catch (IllegalArgumentException exception) {
            diagnose(state, "EDLG-SEM-022", exception.getMessage(), call);
        }
    }

    private Optional<DialogueNodeId> choiceTarget(InvocationExpressionSyntax call,
            NodeContentReader.NodeReadState state) {
        Optional<String> legacy = CallSyntax.namedString(call, "next")
                .or(() -> CallSyntax.positionalString(call, 2));
        if (legacy.isPresent()) {
            return safeNodeId(legacy.orElseThrow(), call, state.context.diagnostics());
        }
        Optional<String> method = CallSyntax.positionalName(call, 2);
        Optional<DialogueNodeId> nodeId = method.map(state.context.methodNodeIds()::get);
        if (method.isPresent() && nodeId.isEmpty()) {
            diagnose(state, "EDLG-SEM-037",
                    "Choice target method is not a dialogue node: " + method.orElseThrow(), call);
        }
        return nodeId;
    }

    private void readComplete(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (state.isTerminalDeclared()) {
            diagnose(state, "EDLG-SEM-023",
                    "Complete cannot be combined with Choice, another Complete or transition.", call);
            return;
        }
        Optional<String> raw = CallSyntax.positionalString(call, 0);
        if (raw.isEmpty()) {
            diagnose(state, "EDLG-SEM-024", "Complete requires DialogueOutcomeId.", call);
            return;
        }
        try {
            state.completion = Optional.of(new CompiledDialogueCompletion(
                    DialogueOutcomeId.parse(raw.orElseThrow())));
        } catch (IllegalArgumentException exception) {
            diagnose(state, "EDLG-SEM-025", exception.getMessage(), call);
        }
    }

    private void readGoTo(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        Optional<String> method = CallSyntax.positionalName(call, 0);
        Optional<DialogueNodeId> target = method.map(state.context.methodNodeIds()::get);
        if (method.isPresent() && target.isPresent()) {
            setTransition(state, call, CompiledDialogueNode.Transition.goTo(target.orElseThrow()));
            return;
        }
        diagnose(state, "ECHO_SCRIPT_NODE_NOT_FOUND", "GoTo requires a known node symbol.", call);
    }

    private void readLoad(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        Optional<String> value = CallSyntax.positionalString(call, 0)
                .or(() -> CallSyntax.positionalName(call, 0)
                        .flatMap(state.context.methods()::fieldStringValue));
        if (value.isEmpty()) {
            diagnose(state, "EDLG-SEM-039",
                    "Dialogue.Load requires DialogueId string or DialogueId field.", call);
            return;
        }
        try {
            setTransition(state, call, CompiledDialogueNode.Transition.load(
                    DialogueId.parse(value.orElseThrow())));
        } catch (IllegalArgumentException exception) {
            diagnose(state, "EDLG-SEM-040", exception.getMessage(), call);
        }
    }

    private void setTransition(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            CompiledDialogueNode.Transition transition) {
        if (state.isTerminalDeclared()) {
            diagnose(state, "EDLG-SEM-041",
                    "Transition cannot be combined with Choice, Complete or another transition.", call);
            return;
        }
        state.transition = Optional.of(transition);
    }

    private Optional<DialogueNodeId> safeNodeId(String raw,
            InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics) {
        try {
            return Optional.of(new DialogueNodeId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-015", exception.getMessage(), call));
            return Optional.empty();
        }
    }

    private void diagnose(NodeContentReader.NodeReadState state, String code, String message,
            InvocationExpressionSyntax call) {
        state.context.diagnostics().add(DialogueSemanticDiagnostics.semantic(code, message, call));
    }

    private java.util.function.Function<String, String> actorResolver(NodeContentReader.NodeReadState state) {
        return actor -> state.context.aliases().resolve(actor)
                .map(binding -> binding.speakerId().toString())
                .orElse(actor);
    }
}
