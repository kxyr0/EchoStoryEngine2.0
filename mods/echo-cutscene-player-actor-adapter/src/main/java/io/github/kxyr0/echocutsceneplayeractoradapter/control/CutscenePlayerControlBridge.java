package io.github.kxyr0.echocutsceneplayeractoradapter.control;

import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneLifecycleEvent;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlMode;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CutscenePlayerControlBridge {
    public void onLifecycle(CutsceneLifecycleEvent event) {
        if (event.status() == CutsceneStatus.PLAYING && event.outcome() == CutsceneOutcome.ACCEPTED) {
            acquire(event);
        }
        if (event.status() == CutsceneStatus.COMPLETED || event.status() == CutsceneStatus.STOPPED
                || event.status() == CutsceneStatus.FAILED) {
            release(event);
        }
    }

    public void onPlayerLoggedOut(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            lookup(serverPlayer).leaseService().ifPresent(service ->
                    service.emergencyRelease(PlayerActorId.of(serverPlayer.getUUID())));
        }
    }

    private void acquire(CutsceneLifecycleEvent event) {
        lookup(event.player()).leaseService().ifPresent(service -> service.acquire(lease(event)));
    }

    private void release(CutsceneLifecycleEvent event) {
        lookup(event.player()).leaseService().ifPresent(service ->
                service.release(PlayerActorId.of(event.player().getUUID()), owner(event)));
    }

    private PlayerControlLease lease(CutsceneLifecycleEvent event) {
        return new PlayerControlLease(new PlayerControlLeaseId("cutscene-player-actor:" + event.playbackId()),
                PlayerActorId.of(event.player().getUUID()), PlayerControlScopeId.CINEMATIC_CONTROL,
                PlayerControlMode.CINEMATIC_CONTROL, owner(event), List.of(), event.player().level().getGameTime(),
                1L, PlayerControlStatus.ACTIVE, 1L);
    }

    private PlayerActorOwnerId owner(CutsceneLifecycleEvent event) {
        return PlayerActorOwnerId.cutscene(event.playbackId().value());
    }

    private EchoPlayerActorServiceBridge.LookupResult lookup(ServerPlayer player) {
        return EchoPlayerActorServiceBridge.lookup(player.server);
    }
}
