package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostorynpcadapter.action.NpcActionTypes;
import io.github.kxyr0.echostorynpcadapter.observe.ObservationBook;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class StoryNpcExtensionSession implements StoryExtensionSession {
    private final MinecraftServer server;
    private final StoryExtensionDescriptor descriptor;
    private final AdapterSessions sessions;
    private final ObservationBook observations = new ObservationBook();
    private final List<StoryActionHandler> handlers;
    private boolean closed;

    StoryNpcExtensionSession(MinecraftServer server, StoryExtensionDescriptor descriptor, AdapterSessions sessions) {
        this.server = Objects.requireNonNull(server, "server");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        NpcActionStarter starter = new NpcActionStarter(server, observations);
        this.handlers = NpcActionTypes.ALL.stream()
                .map(action -> new NpcActionHandler(action, starter))
                .map(StoryActionHandler.class::cast)
                .toList();
        sessions.attach(this);
    }

    MinecraftServer server() {
        return server;
    }

    void tick() {
        if (closed) {
            return;
        }
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        if (lookup.status() == EchoNpcServiceLookupResult.Status.RESOLVED) {
            observations.tick(lookup.service().orElseThrow());
        }
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
        if (closed) {
            return;
        }
        closed = true;
        observations.close();
        sessions.detach(this);
    }
}
