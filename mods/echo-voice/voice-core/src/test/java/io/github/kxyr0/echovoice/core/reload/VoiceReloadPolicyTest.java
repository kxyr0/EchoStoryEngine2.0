package io.github.kxyr0.echovoice.core.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VoiceReloadPolicyTest {
    private final VoiceReloadPolicy policy = new VoiceReloadPolicy();

    @Test
    void validExtensionReplacementCommitsCandidate() {
        assertEquals(VoiceReloadDecision.APPLY_CANDIDATE, policy.decide(1, true, true));
    }

    @Test
    void brokenReplacementKeepsLastKnownGood() {
        assertEquals(VoiceReloadDecision.RETAIN_LAST_KNOWN_GOOD_BROKEN, policy.decide(1, true, false));
    }

    @Test
    void sameStemAmbiguityNeverChoosesArbitrarily() {
        assertEquals(VoiceReloadDecision.RETAIN_LAST_KNOWN_GOOD_AMBIGUOUS, policy.decide(2, true, true));
        assertEquals(VoiceReloadDecision.UNAVAILABLE_AMBIGUOUS, policy.decide(2, false, true));
    }
}
