package io.github.kxyr0.echowardrobe.api.diagnostic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Value plus diagnostics returned by parser, binder, resolution, and validation stages.
 */
public record WardrobeResult<T>(
        Optional<T> value,
        List<WardrobeDiagnostic> diagnostics,
        boolean successful
) {
    public WardrobeResult {
        value = Objects.requireNonNull(value, "value");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (successful && diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            throw new IllegalArgumentException("Successful result must not contain error diagnostics.");
        }
    }

    public static <T> WardrobeResult<T> valid(T value, List<WardrobeDiagnostic> diagnostics) {
        return new WardrobeResult<>(Optional.of(Objects.requireNonNull(value, "value")), diagnostics, true);
    }

    public static <T> WardrobeResult<T> invalid(List<WardrobeDiagnostic> diagnostics) {
        return new WardrobeResult<>(Optional.empty(), diagnostics, false);
    }
}
