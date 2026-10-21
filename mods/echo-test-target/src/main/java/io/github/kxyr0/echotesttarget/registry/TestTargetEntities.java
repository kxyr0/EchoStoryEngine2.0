package io.github.kxyr0.echotesttarget.registry;

import io.github.kxyr0.echotesttarget.EchoTestTargetMod;
import io.github.kxyr0.echotesttarget.entity.ImmortalTestTarget;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TestTargetEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoTestTargetMod.MOD_ID);

    public static final RegistryObject<EntityType<ImmortalTestTarget>> IMMORTAL_TARGET = ENTITIES.register(
            "immortal_target",
            () -> EntityType.Builder.of(ImmortalTestTarget::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(EchoTestTargetMod.MOD_ID + ":immortal_target"));

    private TestTargetEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}

