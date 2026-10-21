package io.github.kxyr0.echotime.script.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import net.minecraft.server.MinecraftServer;

final class TimeStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final TimeStoryWaits waits;
    private final List<StoryActionHandler> handlers;

    TimeStorySession(StoryExtensionDescriptor descriptor, MinecraftServer server) {
        this.descriptor = descriptor;
        waits = new TimeStoryWaits(server);
        handlers = TimeStoryExtensionFactory.ACTIONS.stream()
                .map(type -> (StoryActionHandler) new TimeStoryActionHandler(type, server, waits)).toList();
    }

    @Override public StoryExtensionDescriptor descriptor() { return descriptor; }
    @Override public List<StoryActionHandler> handlers() { return handlers; }
    @Override public void close() { waits.close(); }
}
