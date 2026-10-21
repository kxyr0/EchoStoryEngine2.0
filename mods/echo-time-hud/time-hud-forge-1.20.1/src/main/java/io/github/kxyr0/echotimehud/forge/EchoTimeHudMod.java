package io.github.kxyr0.echotimehud.forge;

import io.github.kxyr0.echotimehud.forge.client.TimeHudClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoTimeHudMod.MOD_ID)
public final class EchoTimeHudMod {
    public static final String MOD_ID = "echo_time_hud";

    public EchoTimeHudMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> TimeHudClientBootstrap.bootstrap(modBus));
    }
}

