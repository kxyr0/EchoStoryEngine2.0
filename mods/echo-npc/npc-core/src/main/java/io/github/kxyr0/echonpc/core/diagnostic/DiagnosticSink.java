package io.github.kxyr0.echonpc.core.diagnostic;

/**
 * Minimal sink so plain core can report diagnostics without choosing a logging backend.
 */
public interface DiagnosticSink {
    void accept(NpcDiagnostic diagnostic);
}
