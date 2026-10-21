package io.github.kxyr0.echonpc.forge.registry;

import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Explicit Forge entity registration for the single N1 custom NPC entity.
 */
public final class EchoNpcEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoNpcMod.MOD_ID);

    public static final RegistryObject<EntityType<EchoNpcEntity>> ECHO_NPC = ENTITIES.register("echo_npc",
            () -> EntityType.Builder.of(EchoNpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(EchoNpcMod.MOD_ID + ":echo_npc"));

    private EchoNpcEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
