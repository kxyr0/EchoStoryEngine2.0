package io.github.kxyr0.echostoryengine.blackspace.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

public final class BlackspaceStoryExtensionFactory implements ForgeStoryExtensionFactory {
    private static final StoryExtensionApiVersion API_VERSION = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            BlackspaceStoryIds.EXTENSION_ID,
            "Echo Blackspace Story Bridge",
            BlackspaceStoryIds.IMPLEMENTATION_VERSION,
            API_VERSION.major(),
            API_VERSION.minor(),
            List.of(action(BlackspaceStoryIds.CREATE), action(BlackspaceStoryIds.TELEPORT_PLAYER)));

    @Override
    public StoryExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public BlackspaceStorySession create(ForgeStoryExtensionContext context) {
        return new BlackspaceStorySession(context.server(), DESCRIPTOR);
    }

    private static ActionHandlerDescriptor action(NamespacedId actionType) {
        return new ActionHandlerDescriptor(actionType, ActionHandlerCapabilities.recoverableAsync());
    }
}
