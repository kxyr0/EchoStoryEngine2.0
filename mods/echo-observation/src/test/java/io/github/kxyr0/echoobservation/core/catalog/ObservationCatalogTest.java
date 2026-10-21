package io.github.kxyr0.echoobservation.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservationCatalogTest {
    @Test
    void invalidReloadRetainsLastKnownGoodCatalog() {
        var resource = new ObservationResourceCompiler().compile(
                "jdh:echo_observation/observations/room.es",
                "class X : ObservationResource { Observation A() { Focus(1); } }")
                .resource().orElseThrow();
        ObservationCatalog catalog = new ObservationCatalog();
        assertTrue(catalog.install(new ObservationCatalogCandidate(List.of(resource), List.of())));
        long revision = catalog.snapshot().revision();
        var error = new ObservationDiagnostic(ObservationDiagnosticCode.OBS_ANCHOR_INVALID,
                ObservationDiagnostic.Severity.ERROR, "broken authoring candidate", 1);

        assertFalse(catalog.install(new ObservationCatalogCandidate(List.of(), List.of(error))));
        assertEquals(revision, catalog.snapshot().revision());
        assertEquals(1, catalog.snapshot().observationCount());
    }
}
