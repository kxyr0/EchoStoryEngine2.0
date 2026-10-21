package io.github.kxyr0.echodialoguecutscene.forge.screen;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class ScreenStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<StoryActionHandler> handlers;

    ScreenStorySession(
            MinecraftServer server,
            StoryExtensionDescriptor descriptor,
            ScreenCommandScheduler scheduler
    ) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.handlers = List.of(new ScreenStoryActionHandler(server, scheduler));
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
