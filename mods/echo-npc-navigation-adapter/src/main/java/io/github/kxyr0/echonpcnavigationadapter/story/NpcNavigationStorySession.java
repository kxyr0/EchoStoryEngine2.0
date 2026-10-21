package io.github.kxyr0.echonpcnavigationadapter.story;

import io.github.kxyr0.echonpcnavigationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class NpcNavigationStorySession implements StoryExtensionSession {
    private final MinecraftServer server;
    private final StoryExtensionDescriptor descriptor;
    private final AdapterSessions sessions;
    private final NpcMoveStoryAction moveAction;
    private final NpcStopStoryAction stopAction;

    NpcNavigationStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor, AdapterSessions sessions) {
        this.server = Objects.requireNonNull(server, "server");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.moveAction = new NpcMoveStoryAction(server);
        this.stopAction = new NpcStopStoryAction(server);
        sessions.attach(this);
    }

    public MinecraftServer server() {
        return server;
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return List.of(moveAction, stopAction);
    }

    public void tick() {
        moveAction.tick();
    }

    @Override
    public void close() {
        moveAction.close();
        sessions.detach(this);
    }
}
