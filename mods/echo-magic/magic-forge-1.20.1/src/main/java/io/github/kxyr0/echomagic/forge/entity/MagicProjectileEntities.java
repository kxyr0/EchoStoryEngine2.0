package io.github.kxyr0.echomagic.forge.entity;

import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MagicProjectileEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoMagicMod.MOD_ID);
    public static final RegistryObject<EntityType<MagicProjectileEntity>> PROJECTILE = ENTITIES.register("projectile",
            () -> EntityType.Builder.<MagicProjectileEntity>of(MagicProjectileEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(10).updateInterval(1)
                    .build(EchoMagicMod.MOD_ID + ":projectile"));
    private MagicProjectileEntities() { }
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
}

