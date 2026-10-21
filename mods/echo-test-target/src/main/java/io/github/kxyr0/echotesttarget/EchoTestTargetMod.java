package io.github.kxyr0.echotesttarget;

import io.github.kxyr0.echotesttarget.command.TestTargetCommands;
import io.github.kxyr0.echotesttarget.entity.ImmortalTestTarget;
import io.github.kxyr0.echotesttarget.registry.TestTargetEntities;
import io.github.kxyr0.echotesttarget.registry.TestTargetItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoTestTargetMod.MOD_ID)
public final class EchoTestTargetMod {
    public static final String MOD_ID = "echo_test_target";

    public EchoTestTargetMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        TestTargetEntities.register(modBus);
        TestTargetItems.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::createAttributes);
        modBus.addListener(TestTargetItems::addToCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SpawnPlacements.register(
                TestTargetEntities.IMMORTAL_TARGET.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules));
    }

    private void createAttributes(EntityAttributeCreationEvent event) {
        event.put(TestTargetEntities.IMMORTAL_TARGET.get(), ImmortalTestTarget.createAttributes().build());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        TestTargetCommands.register(event.getDispatcher());
    }
}
