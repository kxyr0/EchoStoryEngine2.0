package io.github.kxyr0.echoplayeractor.forge.api.view;

import io.github.kxyr0.echoplayeractor.api.diagnostic.PlayerActorDiagnostic;
import java.util.List;

public interface PlayerActorDiagnosticsView {
    List<PlayerActorDiagnostic> recentDiagnostics();
}
