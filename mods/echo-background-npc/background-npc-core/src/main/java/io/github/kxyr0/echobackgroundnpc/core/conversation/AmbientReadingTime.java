package io.github.kxyr0.echobackgroundnpc.core.conversation;

/** Deterministic no-voice reading time policy. */
public record AmbientReadingTime(long minimumMillis, double charactersPerSecond, long voiceTailMillis) {
    public AmbientReadingTime {
        if (minimumMillis < 250L || charactersPerSecond <= 0.0 || voiceTailMillis < 0L) {
            throw new IllegalArgumentException("Invalid ambient timing policy.");
        }
    }
    public static AmbientReadingTime defaults() { return new AmbientReadingTime(1400L, 16.0, 180L); }
    public long forText(String text) {
        return Math.max(minimumMillis, Math.round(text.length() * 1000.0 / charactersPerSecond));
    }
    public long forVoice(long durationMillis) { return Math.max(minimumMillis, durationMillis + voiceTailMillis); }
}
