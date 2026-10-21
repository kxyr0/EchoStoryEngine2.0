package io.github.kxyr0.echotrigger.forge.provider.lifecycle.player;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class PlayerLifecycleEventBridge {
    private final ForgeTriggerServices services;
    private final Map<UUID, PlayerStateSnapshot> snapshots = new LinkedHashMap<>();

    public PlayerLifecycleEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerStateSnapshot current = PlayerStateSnapshot.of(player);
        PlayerStateSnapshot previous = snapshots.put(player.getUUID(), current);
        publishPredicates(player, current);
        if (previous != null) {
            publishStateEdges(player, previous, current);
            publishLevelEdge(player, previous, current);
        }
    }

    @SubscribeEvent
    public void onPlayerDamaged(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "PlayerDamaged", damagePayload(player, event)));
        }
    }

    @SubscribeEvent
    public void onPlayerHealed(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "PlayerHealed", healPayload(player, event.getAmount())));
        }
    }

    @SubscribeEvent
    public void onPlayerDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "PlayerDied", deathPayload(player, event)));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player),
                    "PlayerRespawned", Map.of("dimension", ForgeIds.dimension(player.level()))));
        }
    }

    private void publishPredicates(ServerPlayer player, PlayerStateSnapshot state) {
        publishPredicate(player, "HealthAtMost", Map.of("value", Float.toString(state.health())));
        publishPredicate(player, "HealthAtLeast", Map.of("value", Float.toString(state.health())));
        publishPredicate(player, "FoodAtMost", Map.of("value", Integer.toString(state.food())));
        publishPredicate(player, "FoodAtLeast", Map.of("value", Integer.toString(state.food())));
        publishPredicate(player, "ExperienceLevelAtLeast", Map.of("level", Integer.toString(state.experienceLevel())));
        publishBooleanPredicates(player, state);
    }

    private void publishBooleanPredicates(ServerPlayer player, PlayerStateSnapshot state) {
        publishIf(player, "IsSneaking", state.sneaking());
        publishIf(player, "IsSprinting", state.sprinting());
        publishIf(player, "IsSwimming", state.swimming());
        publishIf(player, "IsSleeping", state.sleeping());
        publishIf(player, "IsOnGround", state.onGround());
        publishIf(player, "IsBurning", state.burning());
    }

    private void publishStateEdges(ServerPlayer player, PlayerStateSnapshot previous, PlayerStateSnapshot current) {
        publishToggle(player, previous.sneaking(), current.sneaking(), "SneakStarted", "SneakStopped");
        publishToggle(player, previous.sprinting(), current.sprinting(), "SprintStarted", "SprintStopped");
        publishToggle(player, previous.swimming(), current.swimming(), "SwimmingStarted", "SwimmingStopped");
        publishToggle(player, previous.sleeping(), current.sleeping(), "SleepStarted", "SleepFinished");
        publishToggle(player, previous.burning(), current.burning(), "PlayerStartedBurning", "PlayerStoppedBurning");
        if (previous.onGround() && !current.onGround() && current.yVelocity() > 0.0D) {
            publish(player, "PlayerJumped", Map.of());
        }
    }

    private void publishLevelEdge(ServerPlayer player, PlayerStateSnapshot previous, PlayerStateSnapshot current) {
        if (current.experienceLevel() > previous.experienceLevel()) {
            publish(player, "ExperienceLevelReached", Map.of(
                    "level", Integer.toString(current.experienceLevel()),
                    "previousLevel", Integer.toString(previous.experienceLevel())));
        }
    }

    private Map<String, String> damagePayload(ServerPlayer player, LivingDamageEvent event) {
        float before = player.getHealth();
        float after = Math.max(0.0F, before - event.getAmount());
        return Map.of("source", ForgeIds.damageSource(event.getSource()),
                "amountAtLeast", Float.toString(event.getAmount()),
                "rawAmount", Float.toString(event.getAmount()),
                "finalAmount", Float.toString(event.getAmount()),
                "healthBefore", Float.toString(before),
                "healthAfter", Float.toString(after));
    }

    private Map<String, String> deathPayload(ServerPlayer player, LivingDeathEvent event) {
        return Map.of("source", ForgeIds.damageSource(event.getSource()),
                "dimension", ForgeIds.dimension(player.level()),
                "position", ForgeIds.position(player.blockPosition()),
                "level", Integer.toString(player.experienceLevel));
    }

    private static Map<String, String> healPayload(ServerPlayer player, float amount) {
        float before = player.getHealth();
        return Map.of("amountAtLeast", Float.toString(amount),
                "healthBefore", Float.toString(before),
                "healthAfter", Float.toString(Math.min(player.getMaxHealth(), before + amount)));
    }

    private void publishToggle(
            ServerPlayer player,
            boolean previous,
            boolean current,
            String started,
            String stopped
    ) {
        if (!previous && current) {
            publish(player, started, Map.of());
        } else if (previous && !current) {
            publish(player, stopped, Map.of());
        }
    }

    private void publishIf(ServerPlayer player, String expressionName, boolean active) {
        if (active) {
            publishPredicate(player, expressionName, Map.of());
        }
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
