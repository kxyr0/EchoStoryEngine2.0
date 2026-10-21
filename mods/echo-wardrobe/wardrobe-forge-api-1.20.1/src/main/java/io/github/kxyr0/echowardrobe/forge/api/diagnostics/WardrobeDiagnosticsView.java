package io.github.kxyr0.echowardrobe.forge.api.diagnostics;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import java.util.List;

/**
 * Query-only public diagnostics view.
 */
public interface WardrobeDiagnosticsView {
    List<WardrobeDiagnostic> diagnostics();
}
