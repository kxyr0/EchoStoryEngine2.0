package io.github.kxyr0.echostorynpcadapter.story;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

/**
 * Server-scoped active Story extension sessions owned by Forge lifecycle.
 */
public final class AdapterSessions {
    private final Map<MinecraftServer, Set<StoryNpcExtensionSession>> sessions = new IdentityHashMap<>();

    void attach(StoryNpcExtensionSession session) {
        sessions.computeIfAbsent(session.server(), ignored -> new LinkedHashSet<>()).add(session);
    }

    void detach(StoryNpcExtensionSession session) {
        Set<StoryNpcExtensionSession> scoped = sessions.get(session.server());
        if (scoped == null) {
            return;
        }
        scoped.remove(session);
        if (scoped.isEmpty()) {
            sessions.remove(session.server());
        }
    }

    public void tick(MinecraftServer server) {
        for (StoryNpcExtensionSession session : Set.copyOf(sessions.getOrDefault(server, Collections.emptySet()))) {
            session.tick();
        }
    }

    public void close(MinecraftServer server) {
        for (StoryNpcExtensionSession session : Set.copyOf(sessions.getOrDefault(
                Objects.requireNonNull(server, "server"), Collections.emptySet()))) {
            session.close();
        }
        sessions.remove(server);
    }
}
