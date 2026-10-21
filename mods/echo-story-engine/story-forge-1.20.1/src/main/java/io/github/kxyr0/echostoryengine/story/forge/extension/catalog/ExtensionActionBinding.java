package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import java.util.Objects;

/**
 * Active owner and handler for one unambiguous action type.
 */
public record ExtensionActionBinding(
        NamespacedId extensionId,
        ActionHandlerCapabilities capabilities,
        StoryActionHandler handler) {
    public ExtensionActionBinding {
        extensionId = Objects.requireNonNull(extensionId, "extensionId");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
        handler = Objects.requireNonNull(handler, "handler");
    }
}
