package io.github.kxyr0.echoscript.api;

/**
 * Immutable value object for the EchoScript language version selected by a parse request.
 *
 * <p>Language versions are independent from artifact, game, Story, Dialogue, or save-schema versions.
 * Unknown positive versions can be represented so the parser can reject them with a structured diagnostic.
 * Instances are immutable and thread-safe.</p>
 */
public record LanguageVersion(int major) {
    public static final LanguageVersion V1 = new LanguageVersion(1);

    public LanguageVersion {
        if (major < 1) {
            throw new IllegalArgumentException("Language version major must be positive.");
        }
    }

    public boolean isV1() {
        return major == 1;
    }

    public static LanguageVersion of(int major) {
        return new LanguageVersion(major);
    }
}
