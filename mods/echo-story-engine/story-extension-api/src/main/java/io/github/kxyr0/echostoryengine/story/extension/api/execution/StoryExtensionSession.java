package io.github.kxyr0.echostoryengine.story.extension.api.execution;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import java.util.List;

/**
 * One active server-scoped extension session.
 */
public interface StoryExtensionSession extends AutoCloseable {
    StoryExtensionDescriptor descriptor();

    List<StoryActionHandler> handlers();

    @Override
    void close();
}
