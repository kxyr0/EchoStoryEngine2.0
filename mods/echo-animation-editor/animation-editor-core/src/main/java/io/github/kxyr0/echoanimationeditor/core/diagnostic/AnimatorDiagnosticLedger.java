package io.github.kxyr0.echoanimationeditor.core.diagnostic;

import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorDiagnostic;
import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorDiagnosticSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AnimatorDiagnosticLedger {
    private final int limit;
    private final List<AnimatorDiagnostic> diagnostics = new ArrayList<>();

    public AnimatorDiagnosticLedger(int limit) {
        this.limit = Math.max(1, limit);
    }

    public void info(String code, String message) {
        add(code, AnimatorDiagnosticSeverity.INFO, message);
    }

    public void warn(String code, String message) {
        add(code, AnimatorDiagnosticSeverity.WARN, message);
    }

    public void error(String code, String message) {
        add(code, AnimatorDiagnosticSeverity.ERROR, message);
    }

    public List<AnimatorDiagnostic> snapshot() {
        return List.copyOf(diagnostics);
    }

    private void add(String code, AnimatorDiagnosticSeverity severity, String message) {
        diagnostics.add(new AnimatorDiagnostic(code, severity, message, Optional.empty()));
        while (diagnostics.size() > limit) {
            diagnostics.remove(0);
        }
    }
}
