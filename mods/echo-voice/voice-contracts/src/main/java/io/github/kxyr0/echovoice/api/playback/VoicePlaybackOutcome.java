package io.github.kxyr0.echovoice.api.playback;

public enum VoicePlaybackOutcome {
    QUEUED,
    STARTED,
    COMPLETED,
    INTERRUPTED,
    REJECTED_DUPLICATE,
    REJECTED_STALE,
    FAILED_ASSET,
    FAILED_DECODE,
    FAILED_AUDIO_DEVICE
}
