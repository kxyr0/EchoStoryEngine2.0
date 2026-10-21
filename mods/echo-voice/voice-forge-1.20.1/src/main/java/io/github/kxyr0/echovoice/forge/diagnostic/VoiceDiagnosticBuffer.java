package io.github.kxyr0.echovoice.forge.diagnostic;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.forge.api.VoiceDiagnosticsView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class VoiceDiagnosticBuffer implements VoiceDiagnosticsView {
    private final int limit;
    private final Deque<VoiceDiagnostic> diagnostics = new ArrayDeque<>();

    public VoiceDiagnosticBuffer(int limit) {
        this.limit = limit;
    }

    public synchronized void add(VoiceDiagnostic diagnostic) {
        diagnostics.addLast(diagnostic);
        while (diagnostics.size() > limit) {
            diagnostics.removeFirst();
        }
    }

    @Override
    public synchronized List<VoiceDiagnostic> recent() {
        return List.copyOf(new ArrayList<>(diagnostics));
    }
}
