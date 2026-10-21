package io.github.kxyr0.echodialoguecutscene.forge.screen;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

public final class ScreenStoryExtensionFactory implements ForgeStoryExtensionFactory {
    private static final StoryExtensionApiVersion API_VERSION = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            ScreenStoryIds.EXTENSION_ID,
            "Echo Screen Story Adapter",
            ScreenStoryIds.IMPLEMENTATION_VERSION,
            API_VERSION.major(),
            API_VERSION.minor(),
            List.of(new ActionHandlerDescriptor(
                    ScreenStoryIds.SET_BLACK, ActionHandlerCapabilities.recoverableAsync())));

    private final ScreenCommandScheduler scheduler;

    public ScreenStoryExtensionFactory(ScreenCommandScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ScreenStorySession create(ForgeStoryExtensionContext context) {
        return new ScreenStorySession(context.server(), DESCRIPTOR, scheduler);
    }
}
