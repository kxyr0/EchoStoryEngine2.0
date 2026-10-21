package io.github.kxyr0.echoeffects.api.diagnostic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EffectResult<T>(Optional<T> value, List<EffectDiagnostic> diagnostics) {
    public EffectResult {
        value = Objects.requireNonNull(value, "value");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static <T> EffectResult<T> valid(T value, List<EffectDiagnostic> diagnostics) {
        return new EffectResult<>(Optional.of(value), diagnostics);
    }

    public static <T> EffectResult<T> invalid(List<EffectDiagnostic> diagnostics) {
        return new EffectResult<>(Optional.empty(), diagnostics);
    }

    public boolean successful() {
        return value.isPresent() && diagnostics.stream().noneMatch(EffectDiagnostic::isError);
    }
}
