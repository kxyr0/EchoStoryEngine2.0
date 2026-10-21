package io.github.kxyr0.echonpc.forge.entity;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public final class EchoNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> ACTOR_ID = stringData();
    private static final EntityDataAccessor<String> DEFINITION_ID = stringData();
    private static final EntityDataAccessor<String> BINDING_ID = stringData();
    private static final EntityDataAccessor<String> DISPLAY_NAME = stringData();
    private static final EntityDataAccessor<String> SKIN_ID = stringData();
    private static final EntityDataAccessor<String> FORM_STATE = stringData();
    private static final EntityDataAccessor<Float> BODY_SCALE = data(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORM_WIDTH = data(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORM_HEIGHT = data(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORM_EYE_HEIGHT = data(EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> SLEEPING = data(EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<String> REST_PROVIDER = stringData();
    static final EntityDataAccessor<String> REST_BED = stringData();
    static final EntityDataAccessor<String> REST_FACING = stringData();
    private static EntityDataAccessor<String> stringData() { return data(EntityDataSerializers.STRING); }
    private static <T> EntityDataAccessor<T> data(EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(EchoNpcEntity.class, serializer);
    }
    public EchoNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(false);
        setInvulnerable(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }
    public void configure(ActorId actorId, EntityBindingId bindingId, NpcDefinition definition) {
        configure(actorId, bindingId, definition, java.util.Optional.empty());
    }
    public void configure(ActorId actorId, EntityBindingId bindingId, NpcDefinition definition,
            java.util.Optional<NpcFormDefinition> form) {
        configure(actorId, bindingId, definition, form, java.util.Optional.empty());
    }
    public void configure(ActorId actorId, EntityBindingId bindingId, NpcDefinition definition,
            java.util.Optional<NpcFormDefinition> form, java.util.Optional<NpcSkinId> skinOverride) {
        entityData.set(ACTOR_ID, actorId.value());
        entityData.set(DEFINITION_ID, definition.id().value());
        entityData.set(BINDING_ID, bindingId.value());
        entityData.set(DISPLAY_NAME, definition.displayName());
        entityData.set(SKIN_ID, definition.skinId().map(Object::toString).orElse(""));
        form.ifPresentOrElse(this::applyForm, () -> clearForm(definition));
        skinOverride.ifPresent(this::applySkin);
        setCustomName(customName(definition));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }
    private Component customName(NpcDefinition definition) {
        Component name = Component.literal(definition.displayName());
        if (definition.nameColor().isEmpty()) {
            return name;
        }
        return name.copy().withStyle(Style.EMPTY.withColor(definition.nameColor().orElseThrow().rgb()));
    }

    public String actorIdValue() { return entityData.get(ACTOR_ID); }
    public String definitionIdValue() { return entityData.get(DEFINITION_ID); }
    public String bindingIdValue() { return entityData.get(BINDING_ID); }
    public String displayNameValue() { return entityData.get(DISPLAY_NAME); }
    public String skinIdValue() { return entityData.get(SKIN_ID); }
    public String formIdValue() { return EchoNpcFormState.field(entityData.get(FORM_STATE), EchoNpcFormState.FORM_ID); }
    public String partTransformsValue() { return EchoNpcFormState.parts(entityData.get(FORM_STATE)); }
    public float bodyScaleValue() { return entityData.get(BODY_SCALE); }
    public boolean sleepingValue() { return EchoNpcRestState.sleeping(entityData); }
    public String restProviderValue() { return EchoNpcRestState.provider(entityData); }
    public String restBedValue() { return EchoNpcRestState.bed(entityData); }
    public String restFacingValue() { return EchoNpcRestState.facing(entityData); }
    public void applySkin(NpcSkinId skinId) { entityData.set(SKIN_ID, skinId.value()); }
    public void markSleeping(NpcRestAttachment attachment) {
        EchoNpcRestState.apply(entityData, attachment);
    }
    public void clearSleeping() {
        EchoNpcRestState.clear(entityData);
    }
    @Override
    protected void registerGoals() {
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTOR_ID, "");
        entityData.define(DEFINITION_ID, "");
        entityData.define(BINDING_ID, "");
        entityData.define(DISPLAY_NAME, "");
        entityData.define(SKIN_ID, "");
        entityData.define(FORM_STATE, "");
        entityData.define(BODY_SCALE, 1.0F);
        entityData.define(FORM_WIDTH, 0.6F);
        entityData.define(FORM_HEIGHT, 1.8F);
        entityData.define(FORM_EYE_HEIGHT, 1.62F);
        EchoNpcRestState.define(entityData);
    }
    @Override
    public Direction getBedOrientation() {
        return EchoNpcRestState.orientation(entityData).orElseGet(super::getBedOrientation);
    }
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(entityData.get(FORM_WIDTH), entityData.get(FORM_HEIGHT));
    }
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return entityData.get(FORM_EYE_HEIGHT);
    }
    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EchoNpcLinkSchema", 1);
        tag.putString("EchoNpcActorId", actorIdValue());
        tag.putString("EchoNpcBindingId", bindingIdValue());
        tag.putString("EchoNpcDefinitionId", definitionIdValue());
        tag.putString("EchoNpcSkinId", skinIdValue());
        tag.putString("EchoNpcFormState", entityData.get(FORM_STATE));
        tag.putFloat("EchoNpcBodyScale", bodyScaleValue());
        tag.putFloat("EchoNpcFormWidth", entityData.get(FORM_WIDTH));
        tag.putFloat("EchoNpcFormHeight", entityData.get(FORM_HEIGHT));
        tag.putFloat("EchoNpcFormEyeHeight", entityData.get(FORM_EYE_HEIGHT));
        EchoNpcRestState.save(entityData, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ACTOR_ID, tag.getString("EchoNpcActorId"));
        entityData.set(DEFINITION_ID, tag.getString("EchoNpcDefinitionId"));
        entityData.set(BINDING_ID, tag.getString("EchoNpcBindingId"));
        if (tag.contains("EchoNpcSkinId")) { entityData.set(SKIN_ID, tag.getString("EchoNpcSkinId")); }
        if (tag.contains("EchoNpcFormState")) { entityData.set(FORM_STATE, tag.getString("EchoNpcFormState")); }
        if (tag.contains("EchoNpcBodyScale")) { entityData.set(BODY_SCALE, tag.getFloat("EchoNpcBodyScale")); }
        if (tag.contains("EchoNpcFormWidth")) { entityData.set(FORM_WIDTH, tag.getFloat("EchoNpcFormWidth")); }
        if (tag.contains("EchoNpcFormHeight")) { entityData.set(FORM_HEIGHT, tag.getFloat("EchoNpcFormHeight")); }
        if (tag.contains("EchoNpcFormEyeHeight")) {
            entityData.set(FORM_EYE_HEIGHT, tag.getFloat("EchoNpcFormEyeHeight"));
        }
        EchoNpcRestState.load(entityData, tag);
        refreshDimensions();
    }

    private void applyForm(NpcFormDefinition form) {
        entityData.set(FORM_STATE, EchoNpcFormState.encode(form));
        entityData.set(SKIN_ID, form.appearance().skinId().map(Object::toString).orElse(skinIdValue()));
        entityData.set(BODY_SCALE, (float) form.bodyGeometry().globalScale());
        entityData.set(FORM_WIDTH, (float) form.dimensions().width());
        entityData.set(FORM_HEIGHT, (float) form.dimensions().height());
        entityData.set(FORM_EYE_HEIGHT, (float) form.dimensions().eyeHeight());
        refreshDimensions();
    }

    private void clearForm(NpcDefinition definition) {
        entityData.set(FORM_STATE, "");
        entityData.set(SKIN_ID, definition.skinId().map(Object::toString).orElse(""));
        entityData.set(BODY_SCALE, 1.0F);
        entityData.set(FORM_WIDTH, 0.6F);
        entityData.set(FORM_HEIGHT, 1.8F);
        entityData.set(FORM_EYE_HEIGHT, 1.62F);
        refreshDimensions();
    }
}
