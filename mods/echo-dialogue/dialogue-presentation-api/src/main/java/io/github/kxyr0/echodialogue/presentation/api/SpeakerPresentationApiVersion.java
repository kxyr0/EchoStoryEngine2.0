package io.github.kxyr0.echodialogue.presentation.api;

/**
 * Version of the public dynamic speaker presentation provider API.
 */
public record SpeakerPresentationApiVersion(int major, int minor) {
    public static final int CURRENT_MAJOR = 1;
    public static final int CURRENT_MINOR = 2;

    public SpeakerPresentationApiVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Presentation API version не может быть отрицательной.");
        }
    }

    public static SpeakerPresentationApiVersion current() {
        return new SpeakerPresentationApiVersion(CURRENT_MAJOR, CURRENT_MINOR);
    }

    public boolean supports(int requiredMajor, int minimumMinor) {
        return major == requiredMajor && minor >= minimumMinor;
    }
}
