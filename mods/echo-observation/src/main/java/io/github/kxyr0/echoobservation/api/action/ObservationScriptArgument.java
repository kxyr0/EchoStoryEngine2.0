package io.github.kxyr0.echoobservation.api.action;

import java.util.Objects;
import java.util.Optional;

public record ObservationScriptArgument(Optional<String> name, String value) {
    public ObservationScriptArgument {
        name = Objects.requireNonNull(name, "name");
        value = Objects.requireNonNull(value, "value");
    }
}
