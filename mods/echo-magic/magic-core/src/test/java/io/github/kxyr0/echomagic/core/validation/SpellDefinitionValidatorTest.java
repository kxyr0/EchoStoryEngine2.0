package io.github.kxyr0.echomagic.core.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.cast.SpellBurstDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellHitDefinition;
import io.github.kxyr0.echomagic.api.definition.SpellProjectileDefinition;
import io.github.kxyr0.echomagic.api.identity.MagicDamageTypeId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.core.SpellTestFixture;
import org.junit.jupiter.api.Test;

class SpellDefinitionValidatorTest {
    @Test void validDefinitionPasses() {
        assertTrue(new SpellDefinitionValidator(SpellSafetyLimits.defaults())
                .validate(SpellTestFixture.definition("one", 1.6), "test").isEmpty());
    }
    @Test void safetyLimitRejectsSpeed() {
        assertFalse(new SpellDefinitionValidator(SpellSafetyLimits.defaults())
                .validate(SpellTestFixture.definition("fast", 9), "test").isEmpty());
    }
    @Test void primitiveContractsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new SpellId("invalid"));
        assertThrows(IllegalArgumentException.class, () -> new SpellProjectileDefinition(0, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpellProjectileDefinition(1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpellBurstDefinition(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpellHitDefinition(new MagicDamageTypeId("test:x"), 0));
    }
}

