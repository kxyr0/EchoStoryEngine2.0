package io.github.kxyr0.echoplayersleep.api.diagnostic;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import java.util.List;
import java.util.Objects;

public record PlayerSleepResult(
        PlayerSleepOutcome outcome,
        List<PlayerSleepDiagnostic> diagnostics) {
    public PlayerSleepResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static PlayerSleepResult success(PlayerSleepOutcome outcome) {
        return new PlayerSleepResult(outcome, List.of());
    }

    public static PlayerSleepResult failure(PlayerSleepOutcome outcome, PlayerSleepDiagnostic diagnostic) {
        return new PlayerSleepResult(outcome, List.of(Objects.requireNonNull(diagnostic, "diagnostic")));
    }
}
