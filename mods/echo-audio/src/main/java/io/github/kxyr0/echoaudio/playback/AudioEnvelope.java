package io.github.kxyr0.echoaudio.playback;

import io.github.kxyr0.echoaudio.api.AudioOptions;

/** Gameplay ticks advance only while Minecraft audio is unpaused; independent of Echo Time and pitch. */
public final class AudioEnvelope {
    private final AudioOptions options;
    private long elapsed;
    private long stopAt = Long.MAX_VALUE;
    private long stopStart;
    private double stopGain;
    public AudioEnvelope(AudioOptions options) { this.options = options; }
    public void tick() { elapsed++; }
    public long elapsed() { return elapsed; }
    public void stop(long fadeTicks) {
        if (stopAt != Long.MAX_VALUE) return;
        stopGain = options.volume() == 0 ? 0 : gain() / options.volume();
        stopStart = elapsed; stopAt = elapsed + fadeTicks;
    }
    public boolean finished() {
        return elapsed >= stopAt || options.durationTicks() > 0 && elapsed >= options.durationTicks();
    }
    public double gain() {
        double gain = options.fadeInTicks() == 0 ? 1 : Math.min(1, (double) elapsed / options.fadeInTicks());
        if (options.durationTicks() > 0 && options.fadeOutTicks() > 0) {
            gain = Math.min(gain, Math.max(0, (double) (options.durationTicks() - elapsed) / options.fadeOutTicks()));
        }
        if (stopAt != Long.MAX_VALUE) {
            gain = stopAt == stopStart ? 0 : stopGain * Math.max(0, (double) (stopAt - elapsed) / (stopAt - stopStart));
        }
        return gain * options.volume();
    }
}
