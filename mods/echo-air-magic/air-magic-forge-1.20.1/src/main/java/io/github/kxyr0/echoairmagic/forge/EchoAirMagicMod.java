package io.github.kxyr0.echoairmagic.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoAirMagicMod.MOD_ID)
public final class EchoAirMagicMod {
    public static final String MOD_ID = "echo_air_magic";

    public EchoAirMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        AirMagicParticles.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent event) {
        new AirMagicCommands().register(event.getDispatcher());
    }
}

