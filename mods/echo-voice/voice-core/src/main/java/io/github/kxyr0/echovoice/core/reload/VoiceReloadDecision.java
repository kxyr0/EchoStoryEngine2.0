package io.github.kxyr0.echovoice.core.reload;

public enum VoiceReloadDecision {
    APPLY_CANDIDATE,
    REMOVE_ASSET,
    RETAIN_LAST_KNOWN_GOOD_AMBIGUOUS,
    RETAIN_LAST_KNOWN_GOOD_BROKEN,
    UNAVAILABLE_AMBIGUOUS
}
