package io.github.kxyr0.echobackgroundnpc.forge.entity;

import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Lightweight render/movement shell. It has no Brain, goals, Mob AI or PathNavigation. */
public final class BackgroundNpcEntity extends Entity {
    private static final EntityDataAccessor<String> AGENT_ID = SynchedEntityData.defineId(
            BackgroundNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> APPEARANCE = SynchedEntityData.defineId(
            BackgroundNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TASK = SynchedEntityData.defineId(
            BackgroundNpcEntity.class, EntityDataSerializers.STRING);
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw, lerpPitch;
    private int lerpSteps;
    private long snapshotRevision = -1L;

    public BackgroundNpcEntity(EntityType<? extends BackgroundNpcEntity> type, Level level) {
        super(type, level);
    }

    public void configure(BackgroundAgentState state) {
        entityData.set(AGENT_ID, state.id().value());
        entityData.set(APPEARANCE, state.appearance().value());
        entityData.set(TASK, state.task().kind().name());
        setUUID(io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcInstanceId
                .stable(state.id()).value());
    }

    public void apply(BackgroundAgentState state) {
        entityData.set(APPEARANCE, state.appearance().value());
        entityData.set(TASK, state.task().kind().name());
    }

    public String agentIdValue() { return entityData.get(AGENT_ID); }
    public String appearanceValue() { return entityData.get(APPEARANCE); }
    public String taskValue() { return entityData.get(TASK); }
    public void setPresentationTask(String task) { entityData.set(TASK, task); }

    public void acceptSnapshot(double x, double y, double z, double velocityX, double velocityY,
            double velocityZ, float facing, String task, long revision, long serverTick) {
        if (revision <= snapshotRevision) return;
        snapshotRevision = revision;
        setDeltaMovement(velocityX, velocityY, velocityZ);
        setPresentationTask(task);
        lerpTo(x, y, z, facing, getXRot(), 3, false);
    }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps,
            boolean teleport) {
        if (!level().isClientSide()) { super.lerpTo(x, y, z, yaw, pitch, steps, teleport); return; }
        lerpX = x; lerpY = y; lerpZ = z; lerpYaw = yaw; lerpPitch = pitch;
        lerpSteps = Math.max(1, steps);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() || lerpSteps <= 0) return;
        double remaining = lerpSteps--;
        setPos(getX() + (lerpX - getX()) / remaining, getY() + (lerpY - getY()) / remaining,
                getZ() + (lerpZ - getZ()) / remaining);
        setYRot(getYRot() + net.minecraft.util.Mth.wrapDegrees(lerpYaw - getYRot()) / (float) remaining);
        setXRot(getXRot() + (lerpPitch - getXRot()) / (float) remaining);
    }

    @Override protected void defineSynchedData() {
        entityData.define(AGENT_ID, "");
        entityData.define(APPEARANCE, "echo_background_npc:default");
        entityData.define(TASK, "STAY");
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(AGENT_ID, tag.getString("BackgroundNpcId"));
        entityData.set(APPEARANCE, tag.getString("BackgroundAppearance"));
        entityData.set(TASK, tag.getString("BackgroundTask"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("BackgroundNpcId", agentIdValue());
        tag.putString("BackgroundAppearance", appearanceValue());
        tag.putString("BackgroundTask", taskValue());
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
}
