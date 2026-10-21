package io.github.kxyr0.echoplayerfollow;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerfollow.client.EchoPlayerFollowClient;
import io.github.kxyr0.echoplayerfollow.network.FollowNetwork;
import io.github.kxyr0.echoplayerfollow.server.FollowServerRuntime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoPlayerFollowMod.MOD_ID)
public final class EchoPlayerFollowMod {
    public static final String MOD_ID = "echo_player_follow";
    public static final String MOD_VERSION = "0.1.0";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final FollowServerRuntime RUNTIME = new FollowServerRuntime();

    public EchoPlayerFollowMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        FollowNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(RUNTIME::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(RUNTIME::onLoggedOut);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EchoPlayerFollowClient.bootstrap(modBus));
    }

    public static FollowServerRuntime runtime() {
        return RUNTIME;
    }
}
