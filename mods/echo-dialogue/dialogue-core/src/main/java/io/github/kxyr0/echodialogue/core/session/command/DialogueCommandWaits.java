package io.github.kxyr0.echodialogue.core.session.command;

import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DialogueCommandWaits {
    private DialogueCommandWaits() {
    }

    public static Optional<DialogueExternalWait> waitFor(
            DialogueSessionState state,
            CompiledDialogueNode node,
            int position
    ) {
        return commandsAt(node, position).stream()
                .findFirst()
                .map(command -> wait(state, node, command));
    }

    public static Optional<DialogueExternalWait> nextAfter(
            DialogueSessionState state,
            CompiledDialogueNode node,
            DialogueExternalWait previous
    ) {
        List<IndexedCommand> commands = commandsAt(node, previous.commandPosition());
        for (int index = 0; index < commands.size(); index++) {
            if (waitKey(state, node, commands.get(index)).equals(previous.waitKey())) {
                return next(commands, index).map(command -> wait(state, node, command));
            }
        }
        return Optional.empty();
    }

    private static DialogueExternalWait wait(
            DialogueSessionState state,
            CompiledDialogueNode node,
            IndexedCommand command
    ) {
        return new DialogueExternalWait(command.command().type(), waitKey(state, node, command),
                DialogueCommandPayload.encode(command.command().arguments()), node.id(), command.command().position());
    }

    private static String waitKey(
            DialogueSessionState state,
            CompiledDialogueNode node,
            IndexedCommand command
    ) {
        return state.sessionId() + ":" + node.id() + ":" + command.command().position() + ":" + command.ordinal()
                + ":" + Integer.toUnsignedString(command.command().arguments().hashCode(), 36);
    }

    private static List<IndexedCommand> commandsAt(CompiledDialogueNode node, int position) {
        List<IndexedCommand> output = new ArrayList<>();
        for (int index = 0; index < node.commands().size(); index++) {
            CompiledDialogueCommand command = node.commands().get(index);
            if (command.position() == position) {
                output.add(new IndexedCommand(index, command));
            }
        }
        return List.copyOf(output);
    }

    private static Optional<IndexedCommand> next(List<IndexedCommand> commands, int index) {
        int nextIndex = index + 1;
        return nextIndex < commands.size() ? Optional.of(commands.get(nextIndex)) : Optional.empty();
    }

    private record IndexedCommand(int ordinal, CompiledDialogueCommand command) {
    }
}
