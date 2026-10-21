package io.github.kxyr0.echoplayerfollow.server;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowPromptPacket;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowStartPacket;
import io.github.kxyr0.echoplayerfollow.network.ClientboundFollowStopPacket;
import io.github.kxyr0.echoplayerfollow.network.FollowNetwork;
import io.github.kxyr0.echoplayerfollow.shared.FollowConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class FollowServerRuntime {
    private final FollowCandidateFinder finder = new FollowCandidateFinder();
    private final Map<UUID, FollowSession> sessions = new HashMap<>();
    private long tick;

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick += 1L;
        if (tick % FollowConstants.SESSION_VALIDATE_INTERVAL_TICKS == 0L) {
            validateSessions(event.getServer());
        }
        if (tick % FollowConstants.AUTO_PROMPT_SCAN_INTERVAL_TICKS == 0L) {
            publishPrompts(event.getServer());
        }
    }

    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        sessions.remove(event.getEntity().getUUID());
    }

    public void start(ServerPlayer player, ActorId actorId) {
        FollowSearchResult result = finder.nearestWithReason(player.getServer(), player,
                FollowConstants.PROMPT_RADIUS, Optional.of(actorId));
        result.candidate().ifPresentOrElse(candidate -> start(player, candidate),
                () -> stop(player, actorId, result.reason()));
    }

    public void probe(ServerPlayer player, int targetEntityId) {
        if (sessions.containsKey(player.getUUID())) {
            FollowNetwork.sendTo(player, ClientboundFollowPromptPacket.empty("already_following"));
            return;
        }
        FollowSearchResult result = finder.targetedWithReason(player.getServer(), player,
                FollowConstants.PROMPT_RADIUS, targetEntityId);
        result.candidate().ifPresentOrElse(candidate -> prompt(player, candidate),
                () -> FollowNetwork.sendTo(player, ClientboundFollowPromptPacket.empty(result.reason())));
    }

    public void cancel(ServerPlayer player, ActorId actorId, String reason) {
        FollowSession current = sessions.get(player.getUUID());
        if (current != null && current.actorId().equals(actorId)) {
            sessions.remove(player.getUUID());
            FollowNetwork.sendTo(player, new ClientboundFollowStopPacket(actorId.value(), reason));
        }
    }

    private void start(ServerPlayer player, FollowCandidate candidate) {
        sessions.put(player.getUUID(), new FollowSession(player.getUUID(), candidate.actorId(),
                candidate.entityId()));
        FollowNetwork.sendTo(player, new ClientboundFollowStartPacket(candidate.entityId(),
                candidate.actorId().value(), candidate.displayName()));
    }

    private void validateSessions(MinecraftServer server) {
        sessions.entrySet().stream().toList().forEach(entry -> validate(server, entry.getKey(), entry.getValue()));
    }

    private void validate(MinecraftServer server, UUID playerId, FollowSession session) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            sessions.remove(playerId);
            return;
        }
        FollowSearchResult result = finder.nearestWithReason(server, player,
                FollowConstants.CANCEL_DISTANCE, Optional.of(session.actorId()));
        result.candidate().ifPresentOrElse(candidate -> refresh(player, session, candidate),
                () -> stop(player, session.actorId(), result.reason()));
    }

    private void refresh(ServerPlayer player, FollowSession session, FollowCandidate candidate) {
        if (session.entityId() == candidate.entityId()) {
            return;
        }
        sessions.put(player.getUUID(), session.withEntityId(candidate.entityId()));
        FollowNetwork.sendTo(player, new ClientboundFollowStartPacket(candidate.entityId(),
                candidate.actorId().value(), candidate.displayName()));
    }

    private void prompt(ServerPlayer player, FollowCandidate candidate) {
        FollowNetwork.sendTo(player, new ClientboundFollowPromptPacket(true, candidate.entityId(),
                candidate.actorId().value(), candidate.displayName(), candidate.distance(), ""));
    }

    private void publishPrompts(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (sessions.containsKey(player.getUUID())) {
                continue;
            }
            FollowSearchResult result = finder.nearestWithReason(server, player,
                    FollowConstants.PROMPT_RADIUS, Optional.empty());
            result.candidate().ifPresent(candidate -> prompt(player, candidate));
        }
    }

    private void stop(ServerPlayer player, ActorId actorId, String reason) {
        sessions.remove(player.getUUID());
        FollowNetwork.sendTo(player, new ClientboundFollowStopPacket(actorId.value(), reason));
    }
}
