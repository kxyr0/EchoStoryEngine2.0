package io.github.kxyr0.echonpcnavigationadapter.runtime;

import io.github.kxyr0.echonpcnavigationadapter.story.NpcNavigationStorySession;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public final class AdapterSessions {
    private final Set<NpcNavigationStorySession> sessions = new LinkedHashSet<>();

    public void attach(NpcNavigationStorySession session) {
        sessions.add(session);
    }

    public void detach(NpcNavigationStorySession session) {
        sessions.remove(session);
    }

    public void tick(MinecraftServer server) {
        sessions.stream().filter(session -> session.server() == server).forEach(NpcNavigationStorySession::tick);
    }

    public void close(MinecraftServer server) {
        sessions.stream().filter(session -> session.server() == server).toList().forEach(NpcNavigationStorySession::close);
    }
}
