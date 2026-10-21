package io.github.kxyr0.echoaudio.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "echo_audio", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AudioReloadRegistration {
    private AudioReloadRegistration() { }
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resources, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor) ->
                AudioClient.instance().catalog().reload().thenCompose(barrier::wait));
    }
}
