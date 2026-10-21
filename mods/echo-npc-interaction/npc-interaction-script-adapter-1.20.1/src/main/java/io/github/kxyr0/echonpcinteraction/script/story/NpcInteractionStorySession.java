package io.github.kxyr0.echonpcinteraction.script.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class NpcInteractionStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final NpcInteractStoryAction interact;

    NpcInteractionStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = descriptor;
        this.interact = new NpcInteractStoryAction(server);
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return List.of(interact, new NpcStopInteractionStoryAction(interact.server()),
                new NpcStandUpStoryAction(interact.server()));
    }

    void tick() {
        interact.tick();
    }

    @Override
    public void close() {
        interact.close();
    }
}
