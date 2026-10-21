package io.github.kxyr0.echoplayer.forge.lifecycle;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.forge.ForgePlayerRuntimeState;
import io.github.kxyr0.echoplayer.forge.data.SavedDataPlayerProfileRepository;
import io.github.kxyr0.echoplayer.forge.log.PlayerStructuredLogger;
import io.github.kxyr0.echoplayer.forge.sync.PlayerProfileSyncChannel;
import java.time.Instant;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Server-thread lifecycle integration for login and logout.
 */
public final class PlayerLoginHooks {
    private final ForgePlayerRuntimeState state;
    private final PlayerProfileSyncChannel syncChannel;
    private final PlayerStructuredLogger logger;

    public PlayerLoginHooks(ForgePlayerRuntimeState state, PlayerProfileSyncChannel syncChannel,
                            PlayerStructuredLogger logger) {
        this.state = Objects.requireNonNull(state, "state");
        this.syncChannel = Objects.requireNonNull(syncChannel, "syncChannel");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            state.useRepository(SavedDataPlayerProfileRepository.open(player.getServer()));
            PlayerProfileSnapshot snapshot = state.lifecycle().login(state.repository(), player.getUUID(),
                    player.getGameProfile().getName(), state.defaultTemplate(), Instant.now());
            state.repository().findAll().forEach(profile -> syncChannel.sendTo(player, profile));
            syncChannel.sendToAll(snapshot);
            logger.info("profile_login", snapshot.playerId().value(), snapshot.profileRevision().value());
        }
    }

    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
            state.useRepository(SavedDataPlayerProfileRepository.open(watcher.getServer()));
            state.services().findByAccountUuid(target.getUUID())
                    .ifPresent(snapshot -> syncChannel.sendTo(watcher, snapshot));
        }
    }

    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getServer() != null) {
            state.useRepository(SavedDataPlayerProfileRepository.open(event.getEntity().getServer()));
        }
        state.services().findByAccountUuid(event.getEntity().getUUID())
                .map(snapshot -> state.lifecycle().logout(state.repository(), snapshot, Instant.now()))
                .ifPresent(snapshot -> logger.info("profile_logout", snapshot.playerId().value(),
                        snapshot.profileRevision().value()));
    }
}
