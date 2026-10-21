package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.view.ActiveEffectView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EffectRuntimeResult(Optional<ActiveEffectView> view, List<EffectDiagnostic> diagnostics) {
    public EffectRuntimeResult {
        view = Objects.requireNonNull(view, "view");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean successful() {
        return view.isPresent() && diagnostics.stream().noneMatch(EffectDiagnostic::isError);
    }
}
