package io.github.kxyr0.echoplayerhud.api.result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Success-or-diagnostics container for HUD operations.
 */
public record HudResult<T>(
        Optional<T> value,
        List<HudDiagnostic> diagnostics
) {
    public HudResult {
        value = Objects.requireNonNull(value, "value");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static <T> HudResult<T> valid(T value, List<HudDiagnostic> diagnostics) {
        return new HudResult<>(Optional.of(Objects.requireNonNull(value, "value")), diagnostics);
    }

    public static <T> HudResult<T> invalid(List<HudDiagnostic> diagnostics) {
        if (diagnostics.stream().noneMatch(HudDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid HUD result requires an error diagnostic.");
        }
        return new HudResult<>(Optional.empty(), diagnostics);
    }
}
