package io.github.kxyr0.echoplayeractor.story.bridge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

public final class PlayerActorStoryExtensionFactory implements ForgeStoryExtensionFactory {
    private static final StoryExtensionApiVersion API_VERSION = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            PlayerActorStoryIds.EXTENSION_ID,
            "Echo Story Player Actor Adapter",
            PlayerActorStoryIds.IMPLEMENTATION_VERSION,
            API_VERSION.major(),
            API_VERSION.minor(),
            List.of(
                    action(PlayerActorStoryIds.SPAWN_AT),
                    action(PlayerActorStoryIds.SET_HOTBAR_ITEM),
                    action(PlayerActorStoryIds.SELECT_HOTBAR_SLOT),
                    action(PlayerActorStoryIds.SIT_AT)));

    @Override
    public StoryExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public PlayerActorStorySession create(ForgeStoryExtensionContext context) {
        return new PlayerActorStorySession(context.server(), DESCRIPTOR);
    }

    private static ActionHandlerDescriptor action(io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId id) {
        return new ActionHandlerDescriptor(id, ActionHandlerCapabilities.recoverableAsync());
    }
}
