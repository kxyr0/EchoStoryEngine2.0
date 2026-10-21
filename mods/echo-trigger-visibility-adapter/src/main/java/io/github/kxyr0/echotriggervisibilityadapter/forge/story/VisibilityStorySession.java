package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceBridge;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class VisibilityStorySession implements StoryExtensionSession {
    private final MinecraftServer server;
    private final StoryExtensionDescriptor descriptor;
    private final List<VisibilityStoryActionHandler> handlers;
    private final Runnable detached;

    VisibilityStorySession(
            MinecraftServer server,
            StoryExtensionDescriptor descriptor,
            Runnable detached) {
        this.server = server;
        this.descriptor = descriptor;
        this.detached = detached;
        handlers = Arrays.stream(VisibilityStoryActionKind.values())
                .map(kind -> new VisibilityStoryActionHandler(server, kind)).toList();
    }

    MinecraftServer server() {
        return server;
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return List.copyOf(handlers);
    }

    void tick() {
        lookup(server).ifPresent(bridge -> handlers.forEach(handler -> handler.tick(bridge)));
    }

    static java.util.Optional<VisibilityServiceBridge> lookup(MinecraftServer server) {
        VisibilityServiceLookupEvent event = new VisibilityServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.bridge();
    }

    @Override
    public void close() {
        handlers.forEach(VisibilityStoryActionHandler::close);
        detached.run();
    }
}
