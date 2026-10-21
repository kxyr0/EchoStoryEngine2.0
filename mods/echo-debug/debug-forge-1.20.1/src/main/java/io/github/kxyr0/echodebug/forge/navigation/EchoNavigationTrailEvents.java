package io.github.kxyr0.echodebug.forge.navigation;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.trail.NpcTrailStatus;
import io.github.kxyr0.echodebug.forge.EchoDebugMod;
import io.github.kxyr0.echodebug.forge.network.ClientboundNpcTrailPacket;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.lifecycle.NavigationLifecycleEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EchoNavigationTrailEvents {
    private static boolean registered;

    private EchoNavigationTrailEvents() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(new EchoNavigationTrailEvents());
            EchoDebugMod.LOGGER.info("Echo Debug NPC movement trail integration registered.");
        }
    }

    @SubscribeEvent
    public void onNavigationLifecycle(NavigationLifecycleEvent event) {
        try {
            EchoDebugNetwork.sendNpcTrail(packet(event.kind(), event.snapshot()));
        } catch (IllegalArgumentException exception) {
            EchoDebugMod.LOGGER.warn("{} moveId={} subjectId={}",
                    DebugDiagnosticCode.DEBUG_NPC_TRAIL_SNAPSHOT_INVALID,
                    event.snapshot().moveId().value(), event.snapshot().subjectId().value(), exception);
        }
    }

    private static ClientboundNpcTrailPacket packet(NavigationLifecycleEvent.Kind kind, NavigationSnapshot snapshot) {
        NavigationProgressSnapshot progress = snapshot.progress();
        Vector3 position = progress.currentPosition();
        return new ClientboundNpcTrailPacket(snapshot.moveId().value(), snapshot.subjectId().value(),
                snapshot.destination().dimensionId(), position.x(), position.y(), position.z(),
                progress.serverTick(), status(kind, snapshot.status()), progress.movementTicks());
    }

    private static NpcTrailStatus status(NavigationLifecycleEvent.Kind kind, NavigationStatus status) {
        return switch (kind) {
            case MOVEMENT_ARRIVED -> NpcTrailStatus.ARRIVED;
            case MOVEMENT_STOPPED -> NpcTrailStatus.CANCELLED;
            case MOVEMENT_FAILED -> NpcTrailStatus.FAILED;
            default -> status(status);
        };
    }

    private static NpcTrailStatus status(NavigationStatus status) {
        return switch (status) {
            case ARRIVED -> NpcTrailStatus.ARRIVED;
            case CANCELLED -> NpcTrailStatus.CANCELLED;
            case BLOCKED, FAILED -> NpcTrailStatus.FAILED;
            default -> NpcTrailStatus.ACTIVE;
        };
    }
}
