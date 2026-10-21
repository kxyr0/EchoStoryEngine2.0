package io.github.kxyr0.echotesttarget.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class ImmortalTestTarget extends Zombie {
    private boolean administrativeRemoval;

    public ImmortalTestTarget(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.echo_test_target.immortal_target"));
        setCustomNameVisible(true);
        setSilent(true);
    }

    @Override
    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isRemoved();
    }

    @Override
    public void setHealth(float value) {
        super.setHealth(getMaxHealth());
    }

    @Override
    public void die(DamageSource source) {
        setHealth(getMaxHealth());
        deathTime = 0;
    }

    @Override
    public void kill() {
        setHealth(getMaxHealth());
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (level().isClientSide || administrativeRemoval || !reason.shouldDestroy()) {
            super.remove(reason);
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void onBelowWorld() {
        if (level() instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            teleportTo(spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D);
            setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void tick() {
        if (isNoAi()) setNoAi(false);
        super.tick();
        if (!isRemoved()) {
            super.setHealth(getMaxHealth());
            deathTime = 0;
        }
    }

    public void removeForCleanup() {
        administrativeRemoval = true;
        discard();
    }
}
