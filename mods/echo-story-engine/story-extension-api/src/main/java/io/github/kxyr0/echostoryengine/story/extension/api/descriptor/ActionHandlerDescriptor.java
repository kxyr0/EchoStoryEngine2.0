package io.github.kxyr0.echostoryengine.story.extension.api.descriptor;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.Objects;

/**
 * Public descriptor for one action type owned by an extension.
 */
public record ActionHandlerDescriptor(
        NamespacedId actionType,
        ActionHandlerCapabilities capabilities) {
    public ActionHandlerDescriptor {
        actionType = Objects.requireNonNull(actionType, "actionType");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }
}
