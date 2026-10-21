package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;

import io.github.kxyr0.echodialogue.echoscript.binding.command.NodeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.function.Function;

final class VisualNodeCallReader implements NodeCallReader {
    private final NodeCommandReader commandReader;

    VisualNodeCallReader(NodeCommandReader commandReader) {
        this.commandReader = commandReader;
    }

    @Override
    public boolean read(
            String rootName,
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        switch (rootName) {
            case "SetFaceAnimation" ->
                    readFaceAnimationCommand(state, call);
            case "ClearFaceAnimation" ->
                    readFaceAnimationClearCommand(state, call, false);
            case "ClearFaceAnimations" ->
                    readFaceAnimationClearCommand(state, call, true);
            case "SetBlinkTiming", "SetIdleBlinkTiming" ->
                    readBlinkTimingCommand(state, call, false);
            case "ClearBlinkTiming", "ClearIdleBlinkTiming" ->
                    readBlinkTimingCommand(state, call, true);
            default -> {
                return false;
            }
        }

        return true;
    }

    private void readFaceAnimationCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-052",
                "SetFaceAnimation command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.faceAnimation(
                        call,
                        state.lines.size(),
                        actorResolver(state),
                        state.context.methods()::fieldStringValue,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readFaceAnimationClearCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            boolean all) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-064",
                "ClearFaceAnimation command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.faceAnimationClear(
                        call,
                        state.lines.size(),
                        actorResolver(state),
                        all,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readBlinkTimingCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            boolean clear) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-054",
                "Blink timing command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.blinkTiming(
                        call,
                        state.lines.size(),
                        actorResolver(state),
                        clear,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private boolean rejectAfterTerminal(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            String code,
            String message) {
        if (!state.isTerminalDeclared()) {
            return false;
        }

        state.context.diagnostics().add(
                DialogueSemanticDiagnostics.semantic(
                        code,
                        message,
                        call));

        return true;
    }

    private Function<String, String> actorResolver(
            NodeContentReader.NodeReadState state) {
        return actor -> state.context.aliases()
                .resolve(actor)
                .map(binding -> binding.speakerId().toString())
                .orElse(actor);
    }
}
