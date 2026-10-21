package io.github.kxyr0.echoanimation.forge.resource;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.playback.AnimationPlaybackHost;
import io.github.kxyr0.echoanimation.forge.resource.service.CatalogAnimationService;
import java.util.ArrayList;
import java.util.List;

/** Holds the last known-good resource catalog. */
public final class AnimationCatalogState {
    private volatile AnimationResourceSet current = new AnimationResourceSet(List.of(), List.of(), List.of());
    private volatile List<AnimationDiagnostic> diagnostics = List.of();
    private AnimationPlaybackHost playbackHost;

    public AnimationResourceSet current() {
        return current;
    }

    public List<AnimationDiagnostic> diagnostics() {
        return diagnostics;
    }

    public void install(AnimationCatalogCandidate candidate) {
        diagnostics = diagnostics(candidate);
        if (candidate.valid()) {
            current = candidate.resourceSet();
        }
    }

    public void bindPlaybackHost(AnimationPlaybackHost host) {
        playbackHost = java.util.Objects.requireNonNull(host, "host");
    }

    public AnimationService service() {
        return new CatalogAnimationService(this, playbackHost);
    }

    private static List<AnimationDiagnostic> diagnostics(AnimationCatalogCandidate candidate) {
        List<AnimationDiagnostic> result = new ArrayList<>(candidate.diagnostics());
        if (!candidate.valid()) {
            result.add(new AnimationDiagnostic(
                    "EANIM-FORGE-CATALOG-RETAINED",
                    DiagnosticSeverity.WARNING,
                    "Предыдущий каталог Echo Animation сохранён из-за ошибок перезагрузки."));
        }
        return List.copyOf(result);
    }
}
