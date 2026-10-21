package io.github.kxyr0.echodebug.forge.creation;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record TriggerCreationResult(
        boolean successful,
        DebugDiagnosticCode code,
        String message,
        Optional<String> triggerId,
        Optional<Path> targetPath,
        Optional<String> sourceFingerprint,
        int parserCalls,
        int binderCalls,
        long catalogRevisionBefore,
        long catalogRevisionAfter
) {
    public TriggerCreationResult {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message").trim();
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        targetPath = Objects.requireNonNull(targetPath, "targetPath");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Creation result message is required.");
        }
    }

    public static TriggerCreationResult failure(DebugDiagnosticCode code, String message) {
        return new TriggerCreationResult(false, code, message, Optional.empty(), Optional.empty(),
                Optional.empty(), 0, 0, 0, 0);
    }
}
