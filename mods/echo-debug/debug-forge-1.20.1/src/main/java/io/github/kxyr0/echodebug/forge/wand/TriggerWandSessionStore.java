package io.github.kxyr0.echodebug.forge.wand;

import io.github.kxyr0.echodebug.core.identity.TriggerWandSessionId;
import io.github.kxyr0.echodebug.core.selection.TriggerWandSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class TriggerWandSessionStore {
    private final Map<UUID, TriggerWandSession> sessions = new HashMap<>();

    public TriggerWandSession session(ServerPlayer player) {
        return sessions.computeIfAbsent(player.getUUID(), id -> new TriggerWandSession(
                TriggerWandSessionId.random(), id.toString()));
    }

    public void remove(UUID playerId) {
        sessions.remove(playerId);
    }
}
