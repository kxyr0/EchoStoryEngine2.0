package io.github.kxyr0.echotrigger.forge.provider.world.dimension;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class DimensionEventBridge {
    private final ForgeTriggerServices services;
    private final Map<UUID, String> gameModes = new LinkedHashMap<>();

    public DimensionEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            publishPredicates(player);
            publishGameModeEdge(player);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publishDimensionEvents(player, event.getFrom(), event.getTo());
        }
    }

    private void publishPredicates(ServerPlayer player) {
        String dimension = ForgeIds.dimension(player.level());
        String gameMode = ForgeIds.gameMode(player.gameMode.getGameModeForPlayer());
        services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                "InDimension", Map.of("target", dimension, "dimension", dimension)));
        services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                "GameModeIs", Map.of("target", gameMode, "mode", gameMode)));
    }

    private void publishGameModeEdge(ServerPlayer player) {
        String current = ForgeIds.gameMode(player.gameMode.getGameModeForPlayer());
        String previous = gameModes.put(player.getUUID(), current);
        if (previous != null && !previous.equals(current)) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "GameModeChanged", Map.of("from", previous, "to", current)));
        }
    }

    private void publishDimensionEvents(
            ServerPlayer player,
            ResourceKey<Level> previous,
            ResourceKey<Level> current
    ) {
        Map<String, String> changed = dimensionPayload(player, previous, current, current);
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                "DimensionChanged", changed));
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                "DimensionLeft", dimensionPayload(player, previous, current, previous)));
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                "DimensionEntered", changed));
    }

    private static Map<String, String> dimensionPayload(
            ServerPlayer player,
            ResourceKey<Level> previous,
            ResourceKey<Level> current,
            ResourceKey<Level> target
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("from", previous.location().toString());
        payload.put("to", current.location().toString());
        payload.put("target", target.location().toString());
        payload.put("dimension", target.location().toString());
        payload.put("position", ForgeIds.position(player.blockPosition()));
        return payload;
    }

    private static long tick(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
}
