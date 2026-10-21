package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

/**
 * Server-scoped Story extension factory registered through the public Forge bridge.
 */
public final class DialogueBridgeExtensionFactory implements ForgeStoryExtensionFactory {
    private static final StoryExtensionApiVersion API_VERSION = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            DialogueBridgeIds.EXTENSION_ID,
            "Echo Story Dialogue Adapter",
            DialogueBridgeIds.IMPLEMENTATION_VERSION,
            API_VERSION.major(),
            API_VERSION.minor(),
            List.of(new ActionHandlerDescriptor(
                    DialogueBridgeIds.ACTION_TYPE, ActionHandlerCapabilities.recoverableAsync())));

    @Override
    public StoryExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public DialogueBridgeSession create(ForgeStoryExtensionContext context) {
        return new DialogueBridgeSession(context.server(), DESCRIPTOR);
    }
}
