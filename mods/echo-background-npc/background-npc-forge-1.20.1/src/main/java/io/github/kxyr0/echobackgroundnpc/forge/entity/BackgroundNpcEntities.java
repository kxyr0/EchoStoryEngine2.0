package io.github.kxyr0.echobackgroundnpc.forge.entity;

import io.github.kxyr0.echobackgroundnpc.forge.EchoBackgroundNpcMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BackgroundNpcEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, EchoBackgroundNpcMod.MOD_ID);
    public static final RegistryObject<EntityType<BackgroundNpcEntity>> BACKGROUND_NPC = REGISTER.register(
            "background_npc", () -> EntityType.Builder.<BackgroundNpcEntity>of(BackgroundNpcEntity::new,
                    MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(6).updateInterval(20)
                    .setShouldReceiveVelocityUpdates(false).build("background_npc"));

    private BackgroundNpcEntities() { }
}
