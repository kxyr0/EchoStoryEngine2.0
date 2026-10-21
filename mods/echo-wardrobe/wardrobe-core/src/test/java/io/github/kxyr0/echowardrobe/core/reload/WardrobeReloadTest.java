package io.github.kxyr0.echowardrobe.core.reload;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WardrobeReloadTest {
    @Test
    void reloadPlannerSeparatesChangedRemovedAndUnchanged() {
        WardrobeReloadPlan plan = new WardrobeReloadPlanner().plan(Map.of("a", "1", "b", "2"),
                Map.of("b", "2", "c", "3"));

        Assertions.assertEquals(List.of("c"), plan.changedSources());
        Assertions.assertEquals(List.of("a"), plan.removedSources());
        Assertions.assertEquals(List.of("b"), plan.unchangedSources());
    }

    @Test
    void lastKnownGoodRetainsCatalogAfterInvalidCandidate() {
        LastKnownGoodCatalog lastKnownGood = new LastKnownGoodCatalog();
        WardrobeResult<WardrobeCatalog> invalid = WardrobeResult.<WardrobeCatalog>invalid(List.of(WardrobeDiagnostic.error(
                WardrobeDiagnosticCode.WARDROBE_DEFINITION_BIND_ERROR, "broken", "bad")));

        WardrobeResult<WardrobeCatalog> retained = lastKnownGood.accept(invalid, "bad");

        Assertions.assertFalse(retained.successful());
        Assertions.assertTrue(retained.value().isPresent());
        Assertions.assertEquals(0L, retained.value().orElseThrow().revision().value());
        Assertions.assertTrue(retained.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_LAST_KNOWN_GOOD_RETAINED));
    }
}
