package io.github.kxyr0.echotrigger.forge.provider.effect.event;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class EffectEventBridge {
    private final ForgeTriggerServices services;

    public EffectEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            player.getActiveEffects().forEach(effect -> publishPredicates(player, effect));
        }
    }

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance current = event.getEffectInstance();
        MobEffectInstance previous = event.getOldEffectInstance();
        if (previous == null) {
            publish(player, "EffectAdded", payload(current, previous));
        } else if (previous.getAmplifier() != current.getAmplifier()) {
            publish(player, "EffectAmplifierChanged", payload(current, previous));
        }
    }

    @SubscribeEvent
    public void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            publish(player, "EffectRemoved", payload(event.getEffect(), event.getEffectInstance()));
        }
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publish(player, "EffectRemoved", payload(event.getEffectInstance(), event.getEffectInstance()));
        }
    }

    private void publishPredicates(ServerPlayer player, MobEffectInstance effect) {
        Map<String, String> payload = payload(effect, effect);
        publishPredicate(player, "HasEffect", payload);
        publishPredicate(player, "EffectAmplifierAtLeast", payload);
        publishPredicate(player, "EffectDurationAtLeast", payload);
    }

    private static Map<String, String> payload(MobEffectInstance current, MobEffectInstance previous) {
        Map<String, String> payload = payload(current.getEffect(), current);
        payload.put("previousAmplifier", previous == null ? "-1" : Integer.toString(previous.getAmplifier()));
        payload.put("newAmplifier", Integer.toString(current.getAmplifier()));
        return payload;
    }

    private static Map<String, String> payload(MobEffect effect, MobEffectInstance instance) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("effect", ForgeIds.effect(effect));
        payload.put("target", ForgeIds.effect(effect));
        payload.put("amplifier", instance == null ? "-1" : Integer.toString(instance.getAmplifier()));
        payload.put("amplifierAtLeast", payload.get("amplifier"));
        payload.put("ticks", instance == null ? "0" : Integer.toString(instance.getDuration()));
        return payload;
    }

    private void publishPredicate(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player), expressionName, payload));
    }

    private void publish(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player), expressionName, payload));
    }

    private static long tick(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
}
