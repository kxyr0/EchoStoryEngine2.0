package io.github.kxyr0.echoobservation.api.action;

import java.util.List;
import java.util.Objects;

public record ObservationScriptAction(String target, List<ObservationScriptArgument> arguments, int sourceOffset) {
    public ObservationScriptAction {
        target = Objects.requireNonNull(target, "target");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (target.isBlank() || sourceOffset < 0) {
            throw new IllegalArgumentException("Invalid compiled Observation action.");
        }
    }
}
