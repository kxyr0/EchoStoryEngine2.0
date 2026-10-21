package io.github.kxyr0.echovoice.forge.api;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import java.util.List;

public interface VoiceDiagnosticsView {
    List<VoiceDiagnostic> recent();
}
