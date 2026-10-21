package io.github.kxyr0.echovoidmagic.epicfight.combat;

import io.github.kxyr0.echovoidmagic.epicfight.EchoVoidMagicEpicFightMod;
import io.github.kxyr0.echovoidmagic.forge.combat.VoidSwordParticleEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.DealDamageEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

@Mod.EventBusSubscriber(modid = EchoVoidMagicEpicFightMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidSwordHitAdapter {
    private static final UUID HIT_LISTENER_ID =
            UUID.fromString("89704736-f3ac-4ed8-904f-0eaaae7bb679");
    private static final Map<UUID, ServerPlayerPatch> REGISTERED_PATCHES = new HashMap<>();
    private static final Map<HitKey, Long> LAST_HITS = new HashMap<>();

    private VoidSwordHitAdapter() { }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || REGISTERED_PATCHES.get(player.getUUID()) == patch) return;
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                HIT_LISTENER_ID, damage -> epicFightDamage(player, damage));
        REGISTERED_PATCHES.put(player.getUUID(), patch);
    }

    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F || !(event.getSource().getEntity() instanceof Player attacker)
                || !(event.getEntity().level() instanceof ServerLevel level)
                || !VoidSwordParticleEffects.holdsVoidSword(attacker)) return;
        spawnOnce(level, event.getEntity(), attacker);
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REGISTERED_PATCHES.remove(event.getEntity().getUUID());
        LAST_HITS.keySet().removeIf(key -> key.attacker().equals(event.getEntity().getUUID()));
    }

    private static void epicFightDamage(ServerPlayer attacker, DealDamageEvent.Damage event) {
        if (event.getAttackDamage() <= 0.0F || !VoidSwordParticleEffects.holdsVoidSword(attacker)
                || !(event.getTarget().level() instanceof ServerLevel level)) return;
        spawnOnce(level, event.getTarget(), attacker);
    }

    private static void spawnOnce(ServerLevel level, LivingEntity target, Player attacker) {
        long gameTime = level.getGameTime();
        HitKey key = new HitKey(attacker.getUUID(), target.getUUID());
        Long previous = LAST_HITS.put(key, gameTime);
        if (previous != null && previous == gameTime) return;
        if (LAST_HITS.size() > 128) LAST_HITS.entrySet().removeIf(entry -> entry.getValue() < gameTime - 2L);
        VoidSwordParticleEffects.spawnHit(level, target, attacker);
    }

    private record HitKey(UUID attacker, UUID target) { }
}
