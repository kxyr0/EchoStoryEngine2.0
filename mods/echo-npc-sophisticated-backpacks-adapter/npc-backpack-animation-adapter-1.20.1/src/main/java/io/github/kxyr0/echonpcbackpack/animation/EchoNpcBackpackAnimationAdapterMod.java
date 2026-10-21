package io.github.kxyr0.echonpcbackpack.animation;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackMarkerId;
import io.github.kxyr0.echonpcbackpack.core.transition.BackpackMarkerSequence;
import io.github.kxyr0.echonpcbackpack.forge.api.NpcBackpackServiceBridge;
import java.util.Set;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(EchoNpcBackpackAnimationAdapterMod.MOD_ID)
public final class EchoNpcBackpackAnimationAdapterMod {
    public static final String MOD_ID = "echo_npc_sophisticated_backpacks_animation_adapter";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> MARKERS = Set.of(BackpackMarkerSequence.TAKE,
            BackpackMarkerSequence.LEFT_HAND, BackpackMarkerSequence.RIGHT_HAND,
            BackpackMarkerSequence.BACK, BackpackMarkerSequence.DETACH, BackpackMarkerSequence.PLACE);
    private final BackpackAnimationBindingCatalog bindings = new BackpackAnimationBindingCatalog();
    private final BackpackAnimationStarter starter = new BackpackAnimationStarter(bindings);

    public EchoNpcBackpackAnimationAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::marker);
        MinecraftForge.EVENT_BUS.addListener(this::reload);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::stopping);
    }

    private void marker(AnimationMarkerEvent event) {
        if (!MARKERS.contains(event.markerId().value())) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) return;
        ActorId actor;
        try {
            actor = new ActorId(event.subjectId().value());
        } catch (IllegalArgumentException exception) {
            return;
        }
        var lookup = NpcBackpackServiceBridge.lookup(server);
        if (lookup.status() != NpcBackpackServiceBridge.Status.RESOLVED) {
            LOGGER.warn("event=backpack_marker_bridge_failed code=ENPC-SB-SERVICE-UNAVAILABLE marker={}",
                    event.markerId());
            return;
        }
        String occurrence = event.playbackId().value() + ":" + event.playbackRevision();
        var result = lookup.service().orElseThrow().marker(actor,
                new NpcBackpackMarkerId(event.markerId().value()), occurrence);
        LOGGER.info("event=backpack_marker actor={} animation={} playback={} marker={} outcome={} code={}",
                actor, event.animationId(), event.playbackId(), event.markerId(),
                result.outcome(), result.diagnosticCode());
    }

    private void reload(AddReloadListenerEvent event) {
        event.addListener(new BackpackAnimationBindingReloadListener(bindings));
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) starter.tick(server);
        }
    }

    private void stopping(ServerStoppingEvent event) {
        starter.clear();
    }
}
