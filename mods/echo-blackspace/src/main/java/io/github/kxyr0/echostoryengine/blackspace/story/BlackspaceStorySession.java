package io.github.kxyr0.echostoryengine.blackspace.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class BlackspaceStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<StoryActionHandler> handlers;

    BlackspaceStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.handlers = List.of(
                new CreateBlackspaceStoryAction(server),
                new TeleportPlayerToBlackspaceStoryAction(server));
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return handlers;
    }

    @Override
    public void close() {
    }
}
