package io.github.kxyr0.echobloodmagic.forge.combat;

import io.github.kxyr0.echobloodmagic.forge.BloodMagicIds;
import io.github.kxyr0.echobloodmagic.forge.item.BloodRapierLease;
import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echomagic.forge.api.event.MagicLinkCutEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class BloodMagicRuntime {
    private static final int MARK_PULSES = 5;
    private static final int MARK_INTERVAL_TICKS = 20;
    private static final float MARK_DAMAGE = 1.0F;
    private static final float RAPIER_BONUS_DAMAGE = 2.0F;
    private static final int RAPIER_DURATION_TICKS = 240;
    private static final ResourceKey<DamageType> BLOOD_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("echo_blood_magic", "blood_magic"));
    private final Map<UUID, BloodMark> marks = new LinkedHashMap<>();
    private final Set<UUID> applyingPulses = new HashSet<>();
    private final BloodRapierCombat rapier = new BloodRapierCombat();

    public void magicCast(MagicLifecycleEvent event) {
        if (!BloodMagicIds.BLOOD_RAPIER.equals(event.spellId())) return;
        event.markHandled();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(event.player().uuid());
        if (player == null) return;
        BloodRapierLease.summon(player, RAPIER_DURATION_TICKS);
        BloodRapierParticles.summon(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.8F, 1.45F);
        player.displayClientMessage(Component.translatable("message.echo_blood_magic.rapier.summoned"), true);
    }

    public void livingDamage(LivingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) return;
        LivingEntity target = event.getEntity();
        if (applyingPulses.contains(target.getUUID())) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player && source.getDirectEntity() == player
                && BloodRapierLease.active(player)) {
            event.setAmount(event.getAmount() + RAPIER_BONUS_DAMAGE);
            BloodMark oldMark = marks.get(target.getUUID());
            if (oldMark != null && player.getUUID().equals(oldMark.caster())) {
                rapier.link(target, player, level.getServer().getTickCount());
            }
            applyMark(target, player, level.getServer().getTickCount());
            BloodRapierParticles.hit(level, target, player.getLookAngle());
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 0.65F, 1.3F);
        } else if (source.is(BLOOD_DAMAGE)) {
            applyMark(target, attacker, level.getServer().getTickCount());
        }
    }

    public void tick(MinecraftServer server) {
        long tick = server.getTickCount();
        tickRapiers(server);
        tickMarks(server, tick);
        rapier.tick(server, tick);
    }

    public boolean rightClick(ServerPlayer player) { return rapier.shatter(player); }
    public void linkCut(MagicLinkCutEvent event) { rapier.cut(event); }
    public void playerLeft(ServerPlayer player) { BloodRapierLease.restore(player); rapier.clear(player.getUUID()); }
    public void playerDied(ServerPlayer player) { playerLeft(player); }
    public void reset() { marks.clear(); applyingPulses.clear(); rapier.reset(); }

    private void tickRapiers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BloodRapierLease.State state = BloodRapierLease.maintain(player);
            if (state == BloodRapierLease.State.EXPIRED) {
                player.displayClientMessage(Component.translatable("message.echo_blood_magic.rapier.faded"), true);
            }
        }
    }

    private void tickMarks(MinecraftServer server, long tick) {
        Iterator<Map.Entry<UUID, BloodMark>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BloodMark> entry = iterator.next();
            BloodMark mark = entry.getValue();
            ServerLevel level = server.getLevel(mark.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }
            if (tick % 10L == 0L) BloodRapierParticles.mark(level, target, false);
            if (tick < mark.nextPulseTick()) continue;
            pulse(level, target, mark);
            if (mark.pulsesLeft() <= 1) iterator.remove();
            else entry.setValue(mark.next(tick + MARK_INTERVAL_TICKS));
        }
    }

    private void pulse(ServerLevel level, LivingEntity target, BloodMark mark) {
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLOOD_DAMAGE);
        Entity caster = mark.caster() == null ? null : level.getEntity(mark.caster());
        applyingPulses.add(target.getUUID());
        boolean applied;
        try { applied = target.hurt(new DamageSource(type, caster), MARK_DAMAGE); }
        finally { applyingPulses.remove(target.getUUID()); }
        if (!applied) return;
        BloodRapierParticles.mark(level, target, true);
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT,
                SoundSource.PLAYERS, 0.22F, 1.75F);
    }

    private void applyMark(LivingEntity target, Entity caster, long tick) {
        UUID casterId = caster == null ? null : caster.getUUID();
        marks.put(target.getUUID(), new BloodMark(target.level().dimension(), casterId,
                tick + MARK_INTERVAL_TICKS, MARK_PULSES));
        if (target.level() instanceof ServerLevel level) BloodRapierParticles.mark(level, target, false);
    }
}
