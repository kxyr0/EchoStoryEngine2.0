package io.github.kxyr0.echowardrobe.core.reload;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Retains the last accepted catalog when a reload candidate is invalid.
 */
public final class LastKnownGoodCatalog {
    private WardrobeCatalog current = WardrobeCatalog.empty();

    public synchronized WardrobeResult<WardrobeCatalog> accept(WardrobeResult<WardrobeCatalog> candidate,
            String source) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.successful()) {
            current = candidate.value().orElseThrow();
            return candidate;
        }
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>(candidate.diagnostics());
        diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_LAST_KNOWN_GOOD_RETAINED,
                "Invalid wardrobe catalog was rejected; previous known-good catalog remains active.", source));
        return new WardrobeResult<>(java.util.Optional.of(current), diagnostics, false);
    }

    public synchronized WardrobeCatalog current() {
        return current;
    }
}
