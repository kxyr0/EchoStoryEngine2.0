package io.github.kxyr0.echoscene.api.definition;

import java.util.List;
import java.util.Objects;

public record SceneStage(int index, List<SceneCommand> commands) {
    public SceneStage {
        if (index < 0) {
            throw new IllegalArgumentException("Stage index must not be negative.");
        }
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("Scene stage must contain commands.");
        }
    }
}
