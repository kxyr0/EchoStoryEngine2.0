package io.github.kxyr0.echomagic.epicfight;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoMagicEpicFightMod.MOD_ID)
public final class EchoMagicEpicFightMod {
    public static final String MOD_ID = "echo_magic_epicfight";

    public EchoMagicEpicFightMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EchoMagicEpicFightParticles.register(modBus);
    }
}
