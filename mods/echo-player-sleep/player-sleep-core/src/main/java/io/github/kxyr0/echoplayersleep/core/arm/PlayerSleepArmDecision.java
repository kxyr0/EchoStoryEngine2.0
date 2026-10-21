package io.github.kxyr0.echoplayersleep.core.arm;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlayerSleepArmDecision(
        boolean accepted,
        Optional<PlayerSleepArmSnapshot> arm,
        List<PlayerSleepDiagnostic> diagnostics) {
    public PlayerSleepArmDecision {
        arm = Objects.requireNonNull(arm, "arm");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static PlayerSleepArmDecision accepted(PlayerSleepArmSnapshot arm) {
        return new PlayerSleepArmDecision(true, Optional.of(Objects.requireNonNull(arm, "arm")), List.of());
    }

    public static PlayerSleepArmDecision rejected(PlayerSleepDiagnostic diagnostic) {
        return new PlayerSleepArmDecision(false, Optional.empty(),
                List.of(Objects.requireNonNull(diagnostic, "diagnostic")));
    }
}
