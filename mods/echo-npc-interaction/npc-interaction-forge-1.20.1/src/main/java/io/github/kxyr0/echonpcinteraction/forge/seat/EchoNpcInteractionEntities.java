package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpcinteraction.forge.EchoNpcInteractionMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EchoNpcInteractionEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoNpcInteractionMod.MOD_ID);

    public static final RegistryObject<EntityType<EchoNpcSeatAnchorEntity>> SEAT_ANCHOR =
            ENTITIES.register("echo_seat_anchor", () -> EntityType.Builder
                    .of(EchoNpcSeatAnchorEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(0)
                    .updateInterval(20)
                    .build(EchoNpcInteractionMod.MOD_ID + ":echo_seat_anchor"));

    private EchoNpcInteractionEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
