package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;
import io.github.kxyr0.echodialogue.echoscript.binding.node.name.CutsceneCommandName;
import io.github.kxyr0.echodialogue.echoscript.binding.node.name.PlayerCommandName;
import io.github.kxyr0.echodialogue.echoscript.binding.node.name.ScreenCommandName;

import io.github.kxyr0.echodialogue.echoscript.binding.command.NodeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Optional;

final class ExternalNodeCallReader implements NodeCallReader {
    private final NodeCommandReader commandReader;

    ExternalNodeCallReader(NodeCommandReader commandReader) {
        this.commandReader = commandReader;
    }

    @Override
    public boolean read(
            String rootName,
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (readAudioCommand(state, call, rootName)) {
            return true;
        }
        Optional<String> cutsceneAction = CutsceneCommandName.action(rootName);

        if (cutsceneAction.isPresent()) {
            readCutsceneCommand(
                    state,
                    call,
                    cutsceneAction.orElseThrow());
            return true;
        }

        Optional<String> playerWaitType = PlayerCommandName.waitType(rootName);

        if (playerWaitType.isPresent()) {
            readPlayerCommand(
                    state,
                    call,
                    playerWaitType.orElseThrow());
            return true;
        }

        Optional<String> screenWaitType = ScreenCommandName.waitType(rootName);

        if (screenWaitType.isPresent()) {
            readScreenCommand(
                    state,
                    call,
                    screenWaitType.orElseThrow());
            return true;
        }

        if ("Effect.ApplyToPlayer".equals(rootName)) {
            readEffectApplyToPlayerCommand(state, call);
            return true;
        }

        if (rootName.startsWith("Time.")) {
            readTimeCommand(state, call, rootName);
            return true;
        }

        return false;
    }

    private boolean readAudioCommand(NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call, String rootName) {
        if (!rootName.startsWith("Sound.")) {
            return false;
        }
        if (!rejectAfterTerminal(state, call, "EDLG-AUDIO-AFTER-TERMINAL",
                "Звук нельзя объявлять после выбора, завершения или перехода.")) {
            new io.github.kxyr0.echodialogue.echoscript.binding.audio.DialogueAudioCommandReader()
                    .read(call, state.lines.size(), state.context.diagnostics()).ifPresent(state.commands::add);
        }
        return true;
    }

    private void readTimeCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            String command) {
        if (rejectAfterTerminal(state, call, "EDLG-SEM-075",
                "Time command cannot be declared after Choice, Complete or transition.")) {
            return;
        }
        commandReader.time(call, state.lines.size(), command, state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readCutsceneCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            String action) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-047",
                "Cutscene command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.cutscene(
                        call,
                        state.lines.size(),
                        action,
                        state.context.methods()::fieldStringValue,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readPlayerCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            String waitType) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-048",
                "Player command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.player(
                        call,
                        state.lines.size(),
                        waitType,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readScreenCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            String waitType) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-051",
                "Screen command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.screen(
                        call,
                        state.lines.size(),
                        waitType,
                        state.context.diagnostics())
                .ifPresent(state.commands::add);
    }

    private void readEffectApplyToPlayerCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (rejectAfterTerminal(
                state,
                call,
                "EDLG-SEM-072",
                "Effect.ApplyToPlayer command cannot be declared after Choice, Complete or transition.")) {
            return;
        }

        commandReader.effectApplyToPlayer(
                        call,
                        state.lines.size(),
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
}
