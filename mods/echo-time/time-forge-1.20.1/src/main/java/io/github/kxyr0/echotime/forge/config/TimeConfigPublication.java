package io.github.kxyr0.echotime.forge.config;

import io.github.kxyr0.echotime.api.config.TimeConfigDiagnostic;
import io.github.kxyr0.echotime.api.config.TimeConfigResult;
import io.github.kxyr0.echotime.api.config.TimeSpeedConfig;
import java.util.List;
import java.util.Objects;

public final class TimeConfigPublication {
    private TimeSpeedConfig active;
    private List<TimeConfigDiagnostic> diagnostics = List.of();

    public TimeConfigPublication(TimeSpeedConfig fallback) {
        active = Objects.requireNonNull(fallback, "fallback");
    }

    public boolean commit(TimeConfigResult candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (!candidate.successful()) {
            diagnostics = candidate.diagnostics();
            return false;
        }
        active = candidate.config().orElseThrow();
        diagnostics = List.of();
        return true;
    }

    public TimeSpeedConfig active() {
        return active;
    }

    public List<TimeConfigDiagnostic> diagnostics() {
        return diagnostics;
    }
}
