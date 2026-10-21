package io.github.kxyr0.echotrigger.forge.provider.progression.event;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class ProgressionEventBridge {
    private final ForgeTriggerServices services;

    public ProgressionEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            player.server.getAdvancements().getAllAdvancements()
                    .forEach(advancement -> publishIfDone(player, advancement));
        }
    }

    @SubscribeEvent
    public void onAdvancementGranted(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "AdvancementGranted", payload(event.getAdvancement())));
        }
    }

    private void publishIfDone(ServerPlayer player, Advancement advancement) {
        if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player),
                    "AdvancementCompleted", payload(advancement)));
        }
    }

    private static Map<String, String> payload(Advancement advancement) {
        String id = advancement.getId().toString();
        return Map.of("advancement", id, "target", id);
    }

    private static long tick(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
}
