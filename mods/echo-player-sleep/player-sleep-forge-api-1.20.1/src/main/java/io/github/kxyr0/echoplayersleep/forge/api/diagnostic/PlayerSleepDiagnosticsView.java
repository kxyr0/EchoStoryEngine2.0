package io.github.kxyr0.echoplayersleep.forge.api.diagnostic;

import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import java.util.List;

public interface PlayerSleepDiagnosticsView {
    List<PlayerSleepDiagnostic> diagnostics();
}
