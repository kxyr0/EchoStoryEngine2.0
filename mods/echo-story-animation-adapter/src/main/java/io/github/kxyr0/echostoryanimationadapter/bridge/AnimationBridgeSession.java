package io.github.kxyr0.echostoryanimationadapter.bridge;

import io.github.kxyr0.echostoryanimationadapter.action.AnimationActionHandler;
import io.github.kxyr0.echostoryanimationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * One active server-scoped Story extension session.
 */
public final class AnimationBridgeSession implements StoryExtensionSession {
    private final MinecraftServer server;
    private final StoryExtensionDescriptor descriptor;
    private final AdapterSessions sessions;
    private final List<AnimationActionHandler> handlers;

    AnimationBridgeSession(
            MinecraftServer server,
            StoryExtensionDescriptor descriptor,
            AdapterSessions sessions
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.handlers = AnimationBridgeIds.ACTION_TYPES.stream()
                .map(action -> new AnimationActionHandler(server, action))
                .toList();
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
        return List.copyOf(handlers);
    }

    public void tick() {
        handlers.forEach(AnimationActionHandler::tick);
    }

    @Override
    public void close() {
        handlers.forEach(AnimationActionHandler::close);
        sessions.detach(this);
    }
}
