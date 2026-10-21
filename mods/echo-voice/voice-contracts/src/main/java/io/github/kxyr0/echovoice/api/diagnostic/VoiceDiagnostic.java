package io.github.kxyr0.echovoice.api.diagnostic;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Stable, queryable and bounded diagnostic payload. */
public record VoiceDiagnostic(
        VoiceDiagnosticCode code,
        Severity severity,
        String message,
        Optional<VoiceAssetKey> assetKey,
        Instant occurredAt
) {
    public VoiceDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message").trim();
        assetKey = Objects.requireNonNull(assetKey, "assetKey");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static VoiceDiagnostic warning(VoiceDiagnosticCode code, String message, VoiceAssetKey key) {
        return new VoiceDiagnostic(code, Severity.WARNING, message, Optional.ofNullable(key), Instant.now());
    }

    public static VoiceDiagnostic error(VoiceDiagnosticCode code, String message, VoiceAssetKey key) {
        return new VoiceDiagnostic(code, Severity.ERROR, message, Optional.ofNullable(key), Instant.now());
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
