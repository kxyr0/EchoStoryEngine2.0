package io.github.kxyr0.echomagic.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.core.SpellTestFixture;
import io.github.kxyr0.echomagic.core.validation.SpellDefinitionValidator;
import io.github.kxyr0.echomagic.core.validation.SpellSafetyLimits;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpellCatalogTest {
    @Test void brokenReloadRetainsPreviousKnownGoodDefinition() {
        SpellCatalog catalog = new SpellCatalog(new SpellDefinitionValidator(SpellSafetyLimits.defaults()));
        assertTrue(catalog.publish(List.of(SpellTestFixture.definition("good", 1.6))).published());
        assertFalse(catalog.publish(List.of(SpellTestFixture.definition("broken", 9.0))).published());
        assertEquals("good", catalog.find(new SpellId("test:spell")).orElseThrow().revision().value());
    }
}
