package io.github.kxyr0.echoaudio.api;

public record AudioOptions(double volume, double pitch, long durationTicks, long fadeInTicks, long fadeOutTicks) {
    public AudioOptions {
        range(volume, 0, 1, "AUDIO_VOLUME_INVALID");
        range(pitch, 0.5, 2, "AUDIO_PITCH_INVALID");
        if (durationTicks < 0 || durationTicks > 72_000) {
            throw new AudioFailure("AUDIO_DURATION_INVALID", "duration must be 0..3600 seconds");
        }
        if (fadeInTicks < 0 || fadeOutTicks < 0 || fadeInTicks > 72_000 || fadeOutTicks > 72_000
                || durationTicks > 0 && (fadeInTicks > durationTicks || fadeOutTicks > durationTicks)) {
            throw new AudioFailure("AUDIO_FADE_INVALID", "fade must be nonnegative and fit duration");
        }
    }
    private static void range(double value, double min, double max, String code) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new AudioFailure(code, "expected " + min + ".." + max + ", got " + value);
        }
    }
}
