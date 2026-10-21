package io.github.kxyr0.echostoryanimationadapter.runtime;

import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeSession;
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
    private final Map<MinecraftServer, Set<AnimationBridgeSession>> sessions = new IdentityHashMap<>();

    public void attach(AnimationBridgeSession session) {
        sessions.computeIfAbsent(session.server(), ignored -> new LinkedHashSet<>()).add(session);
    }

    public void detach(AnimationBridgeSession session) {
        Set<AnimationBridgeSession> scoped = sessions.get(session.server());
        if (scoped == null) {
            return;
        }
        scoped.remove(session);
        if (scoped.isEmpty()) {
            sessions.remove(session.server());
        }
    }

    public void tick(MinecraftServer server) {
        for (AnimationBridgeSession session : Set.copyOf(sessions.getOrDefault(server, Collections.emptySet()))) {
            session.tick();
        }
    }

    public void close(MinecraftServer server) {
        for (AnimationBridgeSession session : Set.copyOf(sessions.getOrDefault(
                Objects.requireNonNull(server, "server"), Collections.emptySet()))) {
            session.close();
        }
        sessions.remove(server);
    }
}
