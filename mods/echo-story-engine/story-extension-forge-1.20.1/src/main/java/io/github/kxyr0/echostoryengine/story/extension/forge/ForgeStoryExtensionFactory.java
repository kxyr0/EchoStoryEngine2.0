package io.github.kxyr0.echostoryengine.story.extension.forge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;

/**
 * Factory registered by an independent Forge mod for one server-scoped Story extension session.
 */
public interface ForgeStoryExtensionFactory {
    StoryExtensionDescriptor descriptor();

    StoryExtensionSession create(ForgeStoryExtensionContext context);
}
