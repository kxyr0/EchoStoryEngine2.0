package io.github.kxyr0.echoplayersleep.forge.service;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.List;

final class ForgePlayerSleepFailures {
    private ForgePlayerSleepFailures() {
    }

    static PlayerSleepResult notActive(List<PlayerSleepDiagnostic> diagnostics, PlayerRef player) {
        return fail(diagnostics, PlayerSleepOutcome.SKIPPED, PlayerSleepFailure.PLAYER_SLEEP_NOT_ARMED,
                player.uuid().toString(), "Player has no active sleep session.");
    }

    static PlayerSleepResult fail(
            List<PlayerSleepDiagnostic> diagnostics,
            PlayerSleepOutcome outcome,
            PlayerSleepFailure code,
            String subject,
            String message) {
        PlayerSleepDiagnostic diagnostic = PlayerSleepDiagnostic.error(code, subject, message);
        diagnostics.add(diagnostic);
        return PlayerSleepResult.failure(outcome, diagnostic);
    }
}
