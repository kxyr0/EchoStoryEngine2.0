package io.github.kxyr0.echomagic.forge.entity;

import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import io.github.kxyr0.echomagic.forge.collision.MagicCollisionResult;
import io.github.kxyr0.echomagic.forge.collision.MagicProjectileCollision;
import io.github.kxyr0.echomagic.forge.sound.MagicSpellSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class MagicProjectileEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(
            MagicProjectileEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private final MagicProjectileCollision collision = new MagicProjectileCollision();
    private double traveled;
    private boolean terminal;

    public MagicProjectileEntity(EntityType<? extends MagicProjectileEntity> type, Level level) { super(type, level); }

    public void configure(MagicProjectilePayload payload, Vec3 origin, Vec3 velocity) {
        entityData.set(DATA, payload.toTag());
        setPos(origin);
        setDeltaMovement(velocity);
    }

    public MagicProjectilePayload payload() { return MagicProjectilePayload.fromTag(entityData.get(DATA)); }

    @Override protected void defineSynchedData() { entityData.define(DATA, new CompoundTag()); }

    @Override public void tick() {
        super.tick();
        if (!entityData.get(DATA).hasUUID("caster")) { if (!level().isClientSide) discard(); return; }
        if (level().isClientSide) { moveClient(); return; }
        MagicProjectilePayload value = payload();
        LivingEntity caster = caster();
        if (caster == null || !caster.isAlive()) { finish(); return; }
        if (value.deflectProjectiles()) deflectProjectiles(value, caster);
        Vec3 previous = position();
        Vec3 next = previous.add(getDeltaMovement());
        MagicCollisionResult hit = collision.resolve(this, previous, next);
        if (hit.hit()) {
            setPos(hit.point());
            hit.entity().ifPresent(target -> EchoMagicMod.services().damage().apply(this, value, target,
                    ((ServerLevel) level()).getGameTime()));
            EchoMagicMod.services().impact(this, hit.point(), value);
            EchoMagicMod.services().find(new SpellId(value.spellId())).ifPresent(definition ->
                    MagicSpellSounds.impact((ServerLevel) level(), hit.point(), definition));
            finish();
            return;
        }
        setPos(next);
        traveled += previous.distanceTo(next);
        if (traveled >= value.range()) { finish(); return; }
        setDeltaMovement(getDeltaMovement().add(0.0D, -value.gravity(), 0.0D));
    }

    public boolean validTarget(LivingEntity target) {
        LivingEntity caster = caster();
        return caster != null && EchoMagicMod.services().targetFilter().valid(caster, target, payload().friendlyFire());
    }

    private LivingEntity caster() {
        if (!(level() instanceof ServerLevel server)) return null;
        Entity value = server.getEntity(payload().caster());
        return value instanceof LivingEntity living ? living : null;
    }

    private void deflectProjectiles(MagicProjectilePayload value, LivingEntity caster) {
        double radius = value.radius() + 0.7D;
        for (Projectile projectile : level().getEntitiesOfClass(Projectile.class,
                getBoundingBox().inflate(radius), candidate -> candidate.getOwner() != caster)) {
            Vec3 movement = projectile.getDeltaMovement();
            double speed = Math.max(0.35D, movement.length());
            Vec3 away = projectile.position().subtract(caster.position());
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
            if (horizontal.lengthSqr() <= 1.0E-8D) {
                Vec3 direction = getDeltaMovement().normalize();
                horizontal = new Vec3(-direction.z, 0.0D, direction.x);
            }
            Vec3 redirected = horizontal.normalize().scale(speed).add(0.0D, 0.12D, 0.0D);
            projectile.setDeltaMovement(redirected);
            projectile.hasImpulse = true;
        }
    }

    private void moveClient() {
        setPos(position().add(getDeltaMovement()));
        setDeltaMovement(getDeltaMovement().add(0.0D, -payload().gravity(), 0.0D));
    }

    private void finish() {
        if (terminal) return;
        terminal = true;
        EchoMagicMod.services().projectileTerminal(new SpellProjectileId(payload().projectileId()));
        discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
