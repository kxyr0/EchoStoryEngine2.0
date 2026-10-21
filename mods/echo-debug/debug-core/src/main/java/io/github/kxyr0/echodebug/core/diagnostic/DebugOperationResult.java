package io.github.kxyr0.echodebug.core.diagnostic;

import java.util.Objects;
import java.util.Optional;

public record DebugOperationResult<T>(
        boolean successful,
        DebugDiagnosticCode code,
        String message,
        Optional<T> value
) {
    public DebugOperationResult {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message").trim();
        value = Objects.requireNonNull(value, "value");
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Result message is required.");
        }
    }

    public static <T> DebugOperationResult<T> success(T value) {
        return new DebugOperationResult<>(true, DebugDiagnosticCode.OK, "OK", Optional.of(value));
    }

    public static <T> DebugOperationResult<T> failure(DebugDiagnosticCode code, String message, T value) {
        return new DebugOperationResult<>(false, code, message, Optional.ofNullable(value));
    }
}
