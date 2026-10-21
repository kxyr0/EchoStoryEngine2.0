package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable compiled dialogue graph node.
 */
public record CompiledDialogueNode(
        DialogueNodeId id,
        List<CompiledDialogueLine> lines,
        List<CompiledDialogueChoice> choices,
        List<CompiledDialogueCommand> commands,
        Optional<CompiledDialogueCompletion> completion,
        Optional<Transition> transition
) {
    public CompiledDialogueNode {
        id = Objects.requireNonNull(id, "id");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        completion = Objects.requireNonNull(completion, "completion");
        transition = Objects.requireNonNull(transition, "transition");
        validateCommands(lines, commands);
        if (!choices.isEmpty() && completion.isPresent()) {
            throw new IllegalArgumentException("Dialogue node cannot contain choices and completion together.");
        }
        if ((!choices.isEmpty() || completion.isPresent()) && transition.isPresent()) {
            throw new IllegalArgumentException(
                    "Dialogue node cannot contain choices/completion and transition together.");
        }
    }

    public CompiledDialogueNode(
            DialogueNodeId id,
            List<CompiledDialogueLine> lines,
            List<CompiledDialogueChoice> choices,
            Optional<CompiledDialogueCompletion> completion
    ) {
        this(id, lines, choices, List.of(), completion, Optional.empty());
    }

    public CompiledDialogueNode(
            DialogueNodeId id,
            List<CompiledDialogueLine> lines,
            List<CompiledDialogueChoice> choices,
            Optional<CompiledDialogueCompletion> completion,
            Optional<Transition> transition
    ) {
        this(id, lines, choices, List.of(), completion, transition);
    }

    public boolean hasChoices() {
        return !choices.isEmpty();
    }

    public boolean hasTransition() {
        return transition.isPresent();
    }

    private static void validateCommands(
            List<CompiledDialogueLine> lines,
            List<CompiledDialogueCommand> commands
    ) {
        for (CompiledDialogueCommand command : commands) {
            if (lines.isEmpty() || command.position() > lines.size()) {
                throw new IllegalArgumentException("Dialogue command position is outside node lines.");
            }
        }
    }

    public record Transition(Optional<DialogueNodeId> targetNodeId, Optional<DialogueId> targetDialogueId) {
        public Transition {
            targetNodeId = Objects.requireNonNull(targetNodeId, "targetNodeId");
            targetDialogueId = Objects.requireNonNull(targetDialogueId, "targetDialogueId");
            if (targetNodeId.isPresent() == targetDialogueId.isPresent()) {
                throw new IllegalArgumentException("Transition must target exactly one node or dialogue.");
            }
        }

        public static Transition goTo(DialogueNodeId targetNodeId) {
            return new Transition(Optional.of(targetNodeId), Optional.empty());
        }

        public static Transition load(DialogueId targetDialogueId) {
            return new Transition(Optional.empty(), Optional.of(targetDialogueId));
        }
    }
}
