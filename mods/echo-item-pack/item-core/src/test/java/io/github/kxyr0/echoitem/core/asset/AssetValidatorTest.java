package io.github.kxyr0.echoitem.core.asset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetValidatorTest {
    @Test
    void textureValidationDoesNotInvokeEchoScriptCounters() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        var result = new TextureCandidateValidator().validate("void_sword.png", png);

        assertTrue(result.valid());
        assertEquals(1, result.counters().pngValidations());
        assertEquals(0, result.counters().parserCalls());
        assertEquals(0, result.counters().binderCalls());
        assertEquals(1, result.counters().resourceReloads());
    }

    @Test
    void brokenModelKeepsPreviousKnownGoodCandidate() {
        var result = new ModelCandidateValidator().validate("void_sword.json", "{broken");

        assertFalse(result.valid());
        assertEquals(1, result.counters().modelValidations());
        assertEquals(0, result.counters().parserCalls());
    }
}
