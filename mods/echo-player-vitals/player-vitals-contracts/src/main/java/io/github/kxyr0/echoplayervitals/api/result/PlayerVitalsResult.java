package io.github.kxyr0.echoplayervitals.api.result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Success-or-diagnostics container for vitals operations.
 */
public record PlayerVitalsResult<T>(
        Optional<T> value,
        List<PlayerVitalsDiagnostic> diagnostics
) {
    public PlayerVitalsResult {
        value = Objects.requireNonNull(value, "value");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static <T> PlayerVitalsResult<T> valid(T value, List<PlayerVitalsDiagnostic> diagnostics) {
        return new PlayerVitalsResult<>(Optional.of(Objects.requireNonNull(value, "value")), diagnostics);
    }

    public static <T> PlayerVitalsResult<T> invalid(List<PlayerVitalsDiagnostic> diagnostics) {
        if (diagnostics.stream().noneMatch(PlayerVitalsDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid vitals result requires an error diagnostic.");
        }
        return new PlayerVitalsResult<>(Optional.empty(), diagnostics);
    }

    public boolean successful() {
        return value.isPresent() && diagnostics.stream().noneMatch(PlayerVitalsDiagnostic::error);
    }
}
