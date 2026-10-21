package io.github.kxyr0.echostoryengine.story.extension.api.descriptor;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.List;
import java.util.Objects;

/**
 * Stable public metadata declared by an external Story extension.
 */
public record StoryExtensionDescriptor(
        NamespacedId extensionId,
        String displayName,
        String implementationVersion,
        int requiredApiMajor,
        int minimumApiMinor,
        List<ActionHandlerDescriptor> actions) {
    public StoryExtensionDescriptor {
        extensionId = Objects.requireNonNull(extensionId, "extensionId");
        displayName = requireText(displayName, "displayName");
        implementationVersion = requireText(implementationVersion, "implementationVersion");
        if (requiredApiMajor < 0 || minimumApiMinor < 0) {
            throw new IllegalArgumentException("API version requirements must be non-negative");
        }
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    public boolean compatibleWith(StoryExtensionApiVersion engineVersion) {
        return Objects.requireNonNull(engineVersion, "engineVersion").supports(requiredApiMajor, minimumApiMinor);
    }

    public String requiredApiText() {
        return requiredApiMajor + "." + minimumApiMinor + "+";
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
