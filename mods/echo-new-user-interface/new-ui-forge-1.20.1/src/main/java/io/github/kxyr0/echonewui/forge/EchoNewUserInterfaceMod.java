package io.github.kxyr0.echonewui.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonewui.forge.client.NewUiClientBootstrap;
import io.github.kxyr0.echonewui.forge.config.NewUiClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoNewUserInterfaceMod.MOD_ID)
public final class EchoNewUserInterfaceMod {
    public static final String MOD_ID = "echo_new_user_interface";
    public static final String MOD_VERSION = "0.1.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoNewUserInterfaceMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NewUiClientConfig.CLIENT_SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NewUiClientBootstrap.bootstrap(modBus));
        LOGGER.info("Echo New User Interface initialized version={}", MOD_VERSION);
    }
}
