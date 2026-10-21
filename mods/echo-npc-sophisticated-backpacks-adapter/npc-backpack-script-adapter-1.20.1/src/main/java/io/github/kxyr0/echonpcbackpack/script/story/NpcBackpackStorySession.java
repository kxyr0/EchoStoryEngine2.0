package io.github.kxyr0.echonpcbackpack.script.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import net.minecraft.server.MinecraftServer;

final class NpcBackpackStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<NpcBackpackStoryAction> actions;

    NpcBackpackStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = descriptor;
        actions = List.of(new NpcBackpackStoryAction(server, NpcBackpackStoryExtensionFactory.EQUIP, true),
                new NpcBackpackStoryAction(server, NpcBackpackStoryExtensionFactory.UNEQUIP, false));
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return List.copyOf(actions);
    }

    void tick() {
        actions.forEach(NpcBackpackStoryAction::tick);
    }

    @Override
    public void close() {
        actions.forEach(NpcBackpackStoryAction::close);
    }
}
