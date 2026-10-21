package io.github.kxyr0.echodialogue.presentation.api;

import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Complete dynamic speaker presentation returned by one provider.
 */
public record ResolvedSpeakerPresentation(
        String displayName,
        Optional<SpeakerNameColor> nameColor,
        Optional<SpeakerVisualRef> visual
) {
    private static final int MAX_DISPLAY_NAME = 96;

    public ResolvedSpeakerPresentation {
        displayName = boundedDisplayName(displayName);
        nameColor = Objects.requireNonNull(nameColor, "nameColor");
        visual = Objects.requireNonNull(visual, "visual");
    }

    public ResolvedSpeakerPresentation(String displayName, Optional<SpeakerVisualRef> visual) {
        this(displayName, Optional.empty(), visual);
    }

    public static ResolvedSpeakerPresentation textOnly(String displayName) {
        return new ResolvedSpeakerPresentation(displayName, Optional.empty(), Optional.empty());
    }

    public static ResolvedSpeakerPresentation textOnly(
            String displayName,
            Optional<SpeakerNameColor> nameColor
    ) {
        return new ResolvedSpeakerPresentation(displayName, nameColor, Optional.empty());
    }

    private static String boundedDisplayName(String value) {
        String trimmed = Objects.requireNonNull(value, "displayName").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Dynamic display name не должен быть пустым.");
        }
        if (trimmed.length() > MAX_DISPLAY_NAME) {
            throw new IllegalArgumentException("Dynamic display name слишком длинный.");
        }
        for (int index = 0; index < trimmed.length(); index++) {
            if (Character.isISOControl(trimmed.charAt(index))) {
                throw new IllegalArgumentException("Dynamic display name содержит control character.");
            }
        }
        return trimmed;
    }
}
