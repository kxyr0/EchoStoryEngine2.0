package io.github.kxyr0.echoplayer.api.result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Success-or-diagnostics container for profile operations.
 */
public record PlayerProfileResult<T>(
        Optional<T> value,
        List<PlayerProfileDiagnostic> diagnostics
) {
    public PlayerProfileResult {
        value = Objects.requireNonNull(value, "value");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static <T> PlayerProfileResult<T> valid(T value, List<PlayerProfileDiagnostic> diagnostics) {
        return new PlayerProfileResult<>(Optional.of(Objects.requireNonNull(value, "value")), diagnostics);
    }

    public static <T> PlayerProfileResult<T> invalid(List<PlayerProfileDiagnostic> diagnostics) {
        if (diagnostics.stream().noneMatch(PlayerProfileDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid result requires at least one error diagnostic.");
        }
        return new PlayerProfileResult<>(Optional.empty(), diagnostics);
    }

    public boolean successful() {
        return value.isPresent() && diagnostics.stream().noneMatch(PlayerProfileDiagnostic::error);
    }
}
