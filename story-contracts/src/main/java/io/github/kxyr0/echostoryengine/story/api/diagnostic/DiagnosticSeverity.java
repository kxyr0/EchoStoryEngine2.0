package io.github.kxyr0.echostoryengine.story.api.diagnostic;

/**
 * Stable severity levels used by Story Engine diagnostics.
 *
 * <p>Adapters may map these levels to a platform logger, command output, or visual diagnostics without changing
 * core behavior.
 */
public enum DiagnosticSeverity {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
