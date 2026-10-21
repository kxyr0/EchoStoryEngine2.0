package io.github.kxyr0.echomagicweapons.forge.combat;

import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import io.github.kxyr0.echomagicweapons.forge.ArsenalIds;
import io.github.kxyr0.echomagicweapons.forge.item.ArsenalItems;
import io.github.kxyr0.echomagicweapons.forge.item.ArsenalWeaponKind;
import io.github.kxyr0.echomagicweapons.forge.item.WeaponLease;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ArsenalRuntime {
    private static final int DURATION_TICKS = 600;
    private final FireWeaponLogic fire = new FireWeaponLogic();
    private final WaterWeaponLogic water = new WaterWeaponLogic();
    private final StoneWeaponLogic stone = new StoneWeaponLogic();
    private final AirWeaponLogic air = new AirWeaponLogic();
    private final VoidWeaponLogic voidMagic = new VoidWeaponLogic();
    private final Set<UUID> specialDamage = new HashSet<>();
    private final Map<UUID, Integer> attackPulses = new HashMap<>();
    private final Map<UUID, Long> swingTicks = new HashMap<>();

    public void magicCast(MagicLifecycleEvent event) {
        ArsenalWeaponKind kind = ArsenalIds.kind(event.spellId());
        if (kind == null) return;
        event.markHandled();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(event.player().uuid());
        if (player == null) return;
        WeaponLease.summon(player, kind, DURATION_TICKS);
        WeaponEffects.dust(player.serverLevel(), player.getEyePosition().add(player.getLookAngle().scale(0.65D)),
                kind.color(), 0.9F, 28, 0.38D);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.8F, 1.25F);
        player.displayClientMessage(Component.translatable("message.echo_magic_weapons.summoned",
                Component.translatable("item.echo_magic_weapons." + kind.id())), true);
    }

    public void livingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (specialDamage.contains(target.getUUID()) || event.getAmount() <= 0.0F) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player) || source.getDirectEntity() != player) return;
        ArsenalWeaponKind kind = ArsenalItems.kind(player.getMainHandItem());
        if (kind == null || !WeaponLease.active(player, kind)) return;
        switch (kind) {
            case FIRE -> fire.hit(player, target);
            case WATER -> water.hit(player, target);
            case STONE -> stone.hit(player, target);
            case AIR -> air.damage(event, player);
            case VOID -> voidMagic.damage(event, player, target);
        }
    }

    public void knockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && WeaponLease.active(player, ArsenalWeaponKind.STONE)) {
            stone.knockback(event, player);
        }
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            WeaponLease.State state = WeaponLease.maintain(player);
            if (state == WeaponLease.State.EXPIRED) {
                player.displayClientMessage(Component.translatable("message.echo_magic_weapons.faded"), true);
                continue;
            }
            ArsenalWeaponKind kind = ArsenalItems.kind(player.getMainHandItem());
            consumeAttackPulse(player, kind);
            if (kind == ArsenalWeaponKind.WATER) water.tick(player);
            else if (kind == ArsenalWeaponKind.STONE) stone.tick(player);
            else if (kind == ArsenalWeaponKind.AIR) air.tick(player);
            else if (kind == ArsenalWeaponKind.VOID) voidMagic.tick(player);
        }
    }

    public void weaponSwing(ServerPlayer player, ArsenalWeaponKind kind) {
        if (!WeaponLease.active(player, kind)) return;
        swingTicks.put(player.getUUID(), player.serverLevel().getGameTime());
        if (kind == ArsenalWeaponKind.FIRE) fire.swing(player);
        else if (kind == ArsenalWeaponKind.WATER) water.swing(player);
    }

    public void specialUse(ServerPlayer player, ArsenalWeaponKind kind) {
        if (!WeaponLease.active(player, kind)) return;
        if (kind == ArsenalWeaponKind.FIRE) fire.plant(player);
        else if (kind == ArsenalWeaponKind.WATER) water.pull(player);
    }

    public void releaseUsing(ServerPlayer player, ArsenalWeaponKind kind, int chargeTicks) {
        if (!WeaponLease.active(player, kind)) return;
        if (kind == ArsenalWeaponKind.AIR) air.release(player, chargeTicks);
        else if (kind == ArsenalWeaponKind.VOID) voidMagic.release(player, chargeTicks);
    }

    public boolean specialDamage(ServerPlayer player, LivingEntity target, float amount) {
        specialDamage.add(target.getUUID());
        try { return target.hurt(player.damageSources().playerAttack(player), amount); }
        finally { specialDamage.remove(target.getUUID()); }
    }

    public void playerLeft(ServerPlayer player) {
        WeaponLease.restore(player);
        UUID id = player.getUUID();
        fire.clear(id);
        water.clear(id);
        voidMagic.clear(id);
        attackPulses.remove(id);
        swingTicks.remove(id);
    }

    public void reset() {
        fire.reset();
        water.reset();
        air.reset();
        voidMagic.reset();
        specialDamage.clear();
        attackPulses.clear();
        swingTicks.clear();
    }

    private void consumeAttackPulse(ServerPlayer player, ArsenalWeaponKind kind) {
        if (kind == null) return;
        int pulse = SummonedWeaponData.attackPulse(player.getMainHandItem());
        Integer previous = attackPulses.put(player.getUUID(), pulse);
        boolean changed = previous == null ? pulse > 0 : pulse != previous;
        long lastSwing = swingTicks.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (changed && player.serverLevel().getGameTime() - lastSwing > 1L) weaponSwing(player, kind);
    }
}
