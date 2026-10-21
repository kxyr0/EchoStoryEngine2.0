package io.github.kxyr0.echowatermagic.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoWaterMagicMod.MOD_ID)
public final class EchoWaterMagicMod {
    public static final String MOD_ID = "echo_water_magic";

    public EchoWaterMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        WaterMagicParticles.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent event) {
        new WaterMagicCommands().register(event.getDispatcher());
    }
}

