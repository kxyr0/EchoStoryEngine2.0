package io.github.kxyr0.echotimehud.forge.config;

import io.github.kxyr0.echotimehud.api.HudConfig;
import java.util.List;
import java.util.Optional;

public record HudConfigResult(Optional<HudConfig> config, List<HudConfigDiagnostic> diagnostics) {
    public HudConfigResult {
        config = config == null ? Optional.empty() : config;
        diagnostics = List.copyOf(diagnostics);
    }
}

