package io.github.kxyr0.echocore;

import io.github.kxyr0.echocore.api.ChapterApi;
import io.github.kxyr0.echocore.client.ChapterClientCommands;
import io.github.kxyr0.echocore.client.ChapterClientRuntime;
import io.github.kxyr0.echocore.client.ChapterConfiguration;
import io.github.kxyr0.echocore.client.MinecraftWorldValidator;
import io.github.kxyr0.echocore.core.ChapterSaveStore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.LoggerFactory;

@Mod("echo_core")
public final class EchoCoreMod {
    public EchoCoreMod() {
        io.github.kxyr0.echocore.features.FeatureNetwork.register();
        if (ModList.get().isLoaded("echo_story_engine")) io.github.kxyr0.echocore.story.CoreStoryBridge.register();
    }
    @Mod.EventBusSubscriber(modid = "echo_core", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                try {
                    var game = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
                    var validator = new MinecraftWorldValidator();
                    Client.runtime = new ChapterClientRuntime(ChapterConfiguration.load(game),
                            new ChapterSaveStore(game.resolve("saves"), validator::validate));
                    ChapterApi.install(Client.runtime);
                    if (ModList.get().isLoaded("fancymenu")) io.github.kxyr0.echocore.fancymenu.FancyMenuBridge.register();
                } catch (Exception failure) { LoggerFactory.getLogger("echo-core").error("Cannot initialize chapter backend", failure); }
            });
        }
    }
    @Mod.EventBusSubscriber(modid = "echo_core", value = Dist.CLIENT)
    public static final class Client {
        private static ChapterClientRuntime runtime;
        @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            io.github.kxyr0.echocore.features.FeatureClientState.clear();
        }
        @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
            if (runtime != null) ChapterClientCommands.register(event, runtime);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (runtime != null && event.phase == TickEvent.Phase.END) runtime.tick();
        }
    }
}
