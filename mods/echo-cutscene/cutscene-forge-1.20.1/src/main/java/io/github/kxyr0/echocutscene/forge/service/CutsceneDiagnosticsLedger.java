package io.github.kxyr0.echocutscene.forge.service;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneDiagnostic;
import io.github.kxyr0.echocutscene.forge.api.diagnostic.CutsceneDiagnosticsView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class CutsceneDiagnosticsLedger implements CutsceneDiagnosticsView {
    private static final int LIMIT = 64;
    private final ArrayDeque<CutsceneDiagnostic> diagnostics = new ArrayDeque<>();

    public void record(CutsceneDiagnostic diagnostic) {
        diagnostics.addLast(diagnostic);
        while (diagnostics.size() > LIMIT) {
            diagnostics.removeFirst();
        }
    }

    @Override
    public List<CutsceneDiagnostic> recent() {
        return List.copyOf(new ArrayList<>(diagnostics));
    }
}
