package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echodialogue.echoscript.binding.CallSyntax;
import io.github.kxyr0.echodialogue.echoscript.binding.MethodCatalog;

import io.github.kxyr0.echodialogue.echoscript.binding.backpack.BackpackDialogueCommandReader;
import io.github.kxyr0.echodialogue.echoscript.binding.command.NodeCommandReader;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final class NpcNodeCallReader implements NodeCallReader {
    private final NodeCommandReader commandReader;
    private final BackpackDialogueCommandReader backpackReader;
    private final Map<String, Handler> handlers;

    NpcNodeCallReader(
            NodeCommandReader commandReader,
            BackpackDialogueCommandReader backpackReader) {
        this.commandReader = commandReader;
        this.backpackReader = backpackReader;
        Map<String, Handler> routes = new HashMap<>();
        routes.put("Npc.Move", (state, call) -> readNpcMovementCommand(state, call, false));
        routes.put("Npc.Sprint", (state, call) -> readNpcMovementCommand(state, call, true));
        routes.put("Npc.WaitForArrival", this::readNpcMoveArrivalCommand);
        routes.put("Npc.Stop", this::readNpcStopCommand);
        routes.put("Npc.Interact", (state, call) -> readNpcInteractionCommand(state, call, true));
        routes.put("Npc.StopInteraction", (state, call) -> readNpcInteractionCommand(state, call, false));
        routes.put("Npc.StandUp", this::readNpcStandUpCommand);
        routes.put("Npc.EquipBackpack", (state, call) -> readNpcBackpackCommand(state, call, true));
        routes.put("Npc.UnequipBackpack", (state, call) -> readNpcBackpackCommand(state, call, false));
        routes.put("Npc.SetForm", this::readNpcSetFormCommand);
        routes.put("Npc.ChangeSkin", this::readNpcChangeSkinCommand);
        routes.put("Npc.SleepAt", this::readNpcSleepAtCommand);
        routes.put("Npc.WakeUp", this::readNpcWakeUpCommand);
        handlers = Map.copyOf(routes);
    }

    @Override
    public boolean read(
            String rootName,
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        Handler handler = handlers.get(rootName);
        if (handler == null) {
            return false;
        }
        handler.read(state, call);
        return true;
    }

    private void readNpcMovementCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            boolean sprint) {
        if (reject(state, call, "EDLG-SEM-044", "Npc command cannot follow a terminal declaration.")) {
            return;
        }
        var command = sprint
                ? commandReader.sprint(call, position(state), actorResolver(state), diagnostics(state))
                : commandReader.move(call, position(state), actorResolver(state), diagnostics(state));
        command.ifPresent(state.commands::add);
    }

    private void readNpcMoveArrivalCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-044", "Npc.WaitForArrival cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.moveArrival(call, position(state), actorResolver(state), diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcStopCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-044", "Npc command cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.stop(call, position(state), actorResolver(state), diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcInteractionCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            boolean interact) {
        if (reject(state, call, "EDLG-SEM-068", "Npc interaction cannot follow a terminal declaration.")) {
            return;
        }
        var command = interact
                ? commandReader.interact(call, position(state), actorResolver(state),
                        state.context.methods()::fieldBlockPosition, diagnostics(state))
                : commandReader.stopInteraction(call, position(state), actorResolver(state), diagnostics(state));
        command.ifPresent(state.commands::add);
    }

    private void readNpcStandUpCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-070", "Npc.StandUp cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.standUp(call, position(state), actorResolver(state), diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcBackpackCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call,
            boolean equip) {
        if (reject(state, call, "EDLG-SEM-072", "Backpack command cannot follow a terminal declaration.")) {
            return;
        }
        var context = new BackpackDialogueCommandReader.ReadContext(position(state), actorResolver(state),
                state.context.methods()::fieldBlockPosition, diagnostics(state));
        backpackReader.read(call, equip, context).ifPresent(state.commands::add);
    }

    private void readNpcSetFormCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-058", "Npc.SetForm cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.setForm(call, position(state), actorResolver(state),
                        state.context.methods()::fieldStringValue, diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcChangeSkinCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-076", "Npc.ChangeSkin cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.changeSkin(call, position(state), actorResolver(state),
                        state.context.methods()::fieldStringValue, diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcSleepAtCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-062", "Npc.SleepAt cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.sleepAt(call, position(state), actorResolver(state),
                        state.context.methods()::fieldStringValue, diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private void readNpcWakeUpCommand(
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call) {
        if (reject(state, call, "EDLG-SEM-063", "Npc.WakeUp cannot follow a terminal declaration.")) {
            return;
        }
        commandReader.wakeUp(call, position(state), actorResolver(state), diagnostics(state))
                .ifPresent(state.commands::add);
    }

    private boolean reject(NodeContentReader.NodeReadState state, InvocationExpressionSyntax call,
            String code, String message) {
        if (!state.isTerminalDeclared()) {
            return false;
        }
        diagnostics(state).add(DialogueSemanticDiagnostics.semantic(code, message, call));
        return true;
    }

    private int position(NodeContentReader.NodeReadState state) {
        return state.lines.size();
    }

    private java.util.List<io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic> diagnostics(
            NodeContentReader.NodeReadState state) {
        return state.context.diagnostics();
    }

    private Function<String, String> actorResolver(
            NodeContentReader.NodeReadState state) {
        return actor -> state.context.aliases()
                .resolve(actor)
                .map(binding -> binding.speakerId().toString())
                .orElse(actor);
    }

    @FunctionalInterface
    private interface Handler {
        void read(NodeContentReader.NodeReadState state, InvocationExpressionSyntax call);
    }
}
