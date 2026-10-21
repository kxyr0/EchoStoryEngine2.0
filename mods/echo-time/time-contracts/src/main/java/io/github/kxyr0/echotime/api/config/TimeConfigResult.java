package io.github.kxyr0.echotime.api.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TimeConfigResult(Optional<TimeSpeedConfig> config, List<TimeConfigDiagnostic> diagnostics) {
    public TimeConfigResult {
        config = Objects.requireNonNull(config, "config");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return config.isPresent() && diagnostics.isEmpty();
    }
}
