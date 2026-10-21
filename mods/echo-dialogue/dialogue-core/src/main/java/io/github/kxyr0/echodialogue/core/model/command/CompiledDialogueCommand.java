package io.github.kxyr0.echodialogue.core.model.command;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable external command compiled from dialogue node source.
 */
public record CompiledDialogueCommand(
        String type,
        int position,
        Map<String, String> arguments
) {
    public CompiledDialogueCommand {
        type = Objects.requireNonNull(type, "type").trim();
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Dialogue command type must not be blank.");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Dialogue command position must not be negative.");
        }
    }
}
