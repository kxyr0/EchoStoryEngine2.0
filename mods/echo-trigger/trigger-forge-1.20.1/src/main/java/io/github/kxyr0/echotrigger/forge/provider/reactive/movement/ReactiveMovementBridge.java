package io.github.kxyr0.echotrigger.forge.provider.reactive.movement;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.core.runtime.reactive.TriggerReactiveRuntime;
import io.github.kxyr0.echotrigger.forge.api.callback.TriggerCallbackInvocationEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class ReactiveMovementBridge {
    private final ForgeTriggerServices services;
    private final Map<UUID, PlayerMovementSnapshot> snapshots = new LinkedHashMap<>();

    public ReactiveMovementBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerMovementSnapshot current = PlayerMovementSnapshot.of(player);
        PlayerMovementSnapshot previous = snapshots.put(player.getUUID(), current);
        publish(player, previous == null ? current : previous, current);
    }

    private void publish(
            ServerPlayer player,
            PlayerMovementSnapshot previous,
            PlayerMovementSnapshot current
    ) {
        TriggerOwnerId owner = ForgeIds.owner(player);
        services.applyMovement(new TriggerReactiveRuntime.Movement(owner, previous.dimension(), current.dimension(),
                previous.position(), current.position(), player.serverLevel().getGameTime()))
                .forEach(ReactiveMovementBridge::post);
    }

    private static void post(TriggerCallbackInvocation invocation) {
        MinecraftForge.EVENT_BUS.post(new TriggerCallbackInvocationEvent(invocation));
    }
}
