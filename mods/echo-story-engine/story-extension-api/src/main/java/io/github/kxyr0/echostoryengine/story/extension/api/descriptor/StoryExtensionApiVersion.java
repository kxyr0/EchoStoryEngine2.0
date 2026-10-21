package io.github.kxyr0.echostoryengine.story.extension.api.descriptor;

/**
 * Version of the stable Story Extension API understood by this engine line.
 */
public record StoryExtensionApiVersion(int major, int minor) {
    public static final int CURRENT_MAJOR = 1;
    public static final int CURRENT_MINOR = 0;

    public StoryExtensionApiVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("version numbers must be non-negative");
        }
    }

    public static StoryExtensionApiVersion current() {
        return new StoryExtensionApiVersion(CURRENT_MAJOR, CURRENT_MINOR);
    }

    public boolean supports(int requiredMajor, int minimumMinor) {
        return major == requiredMajor && minor >= minimumMinor;
    }

    public String asText() {
        return major + "." + minor;
    }
}
