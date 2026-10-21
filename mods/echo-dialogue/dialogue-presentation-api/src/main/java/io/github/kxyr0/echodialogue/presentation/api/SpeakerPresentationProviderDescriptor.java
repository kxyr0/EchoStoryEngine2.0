package io.github.kxyr0.echodialogue.presentation.api;

import java.util.Objects;

/**
 * Public metadata declared by one provider factory before activation.
 */
public record SpeakerPresentationProviderDescriptor(
        SpeakerPresentationProviderId id,
        String displayName,
        String implementationVersion,
        int requiredApiMajor,
        int minimumApiMinor
) {
    private static final int MAX_DISPLAY_NAME = 96;
    private static final int MAX_IMPLEMENTATION_VERSION = 64;

    public SpeakerPresentationProviderDescriptor {
        id = Objects.requireNonNull(id, "id");
        displayName = boundedText(displayName, "displayName", MAX_DISPLAY_NAME);
        implementationVersion = boundedText(
                implementationVersion, "implementationVersion", MAX_IMPLEMENTATION_VERSION);
        if (requiredApiMajor < 0 || minimumApiMinor < 0) {
            throw new IllegalArgumentException("Presentation API requirement не может быть отрицательным.");
        }
    }

    public SpeakerPresentationProviderDescriptor(
            SpeakerPresentationProviderId id,
            String displayName,
            String implementationVersion
    ) {
        this(id, displayName, implementationVersion,
                SpeakerPresentationApiVersion.CURRENT_MAJOR,
                SpeakerPresentationApiVersion.CURRENT_MINOR);
    }

    public boolean compatibleWith(SpeakerPresentationApiVersion version) {
        return Objects.requireNonNull(version, "version").supports(requiredApiMajor, minimumApiMinor);
    }

    private static String boundedText(String value, String name, int maxLength) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " не должен быть пустым.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " слишком длинный.");
        }
        return trimmed;
    }
}
