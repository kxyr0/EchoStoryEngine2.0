package io.github.kxyr0.echoseating.forge.api.view;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import java.util.List;

public interface SeatingDiagnosticsView {
    List<SeatingDiagnostic> diagnostics();
}
