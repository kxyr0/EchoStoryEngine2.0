package io.github.kxyr0.echovoice.core.reload;

/** Format ambiguity and last-known-good decisions independent of filesystem code. */
public final class VoiceReloadPolicy {
    public VoiceReloadDecision decide(int supportedCandidateCount, boolean knownGood, boolean decodeSuccessful) {
        if (supportedCandidateCount == 0) {
            return VoiceReloadDecision.REMOVE_ASSET;
        }
        if (supportedCandidateCount > 1) {
            return knownGood ? VoiceReloadDecision.RETAIN_LAST_KNOWN_GOOD_AMBIGUOUS
                    : VoiceReloadDecision.UNAVAILABLE_AMBIGUOUS;
        }
        if (!decodeSuccessful) {
            return knownGood ? VoiceReloadDecision.RETAIN_LAST_KNOWN_GOOD_BROKEN
                    : VoiceReloadDecision.REMOVE_ASSET;
        }
        return VoiceReloadDecision.APPLY_CANDIDATE;
    }
}
