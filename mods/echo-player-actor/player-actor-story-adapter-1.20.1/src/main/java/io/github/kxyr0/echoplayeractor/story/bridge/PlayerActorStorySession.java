package io.github.kxyr0.echoplayeractor.story.bridge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class PlayerActorStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<StoryActionHandler> actions;

    PlayerActorStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.actions = List.of(
                new PlayerActorStoryAction(server, PlayerActorStoryIds.SPAWN_AT),
                new PlayerActorStoryAction(server, PlayerActorStoryIds.SET_HOTBAR_ITEM),
                new PlayerActorStoryAction(server, PlayerActorStoryIds.SELECT_HOTBAR_SLOT),
                new PlayerActorStoryAction(server, PlayerActorStoryIds.SIT_AT));
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return actions;
    }

    @Override
    public void close() {
    }
}
