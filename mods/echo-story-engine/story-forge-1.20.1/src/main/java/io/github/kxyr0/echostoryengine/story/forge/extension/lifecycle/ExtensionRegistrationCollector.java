package io.github.kxyr0.echostoryengine.story.forge.extension.lifecycle;

import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionRegistrar;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable registration surface that is closed immediately after the Forge event returns.
 */
public final class ExtensionRegistrationCollector implements ForgeStoryExtensionRegistrar {
    private final List<ForgeStoryExtensionFactory> factories = new ArrayList<>();
    private boolean open = true;

    @Override
    public boolean register(ForgeStoryExtensionFactory factory) {
        Objects.requireNonNull(factory, "factory");
        if (!open) {
            return false;
        }
        factories.add(factory);
        return true;
    }

    public List<ForgeStoryExtensionFactory> closeAndList() {
        open = false;
        return List.copyOf(factories);
    }
}
