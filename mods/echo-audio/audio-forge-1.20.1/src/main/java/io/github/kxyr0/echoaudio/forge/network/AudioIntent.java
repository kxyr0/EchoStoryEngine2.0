package io.github.kxyr0.echoaudio.forge.network;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.api.AudioPlaybackId;

public record AudioIntent(AudioPlaybackId id, String owner, AudioCue cue, String dimension,
                          double x, double y, double z) {
    public boolean spatial() { return !dimension.isEmpty(); }
}
