package io.github.kxyr0.echoaudio.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "echo_audio", value = Dist.CLIENT)
public final class AudioClientEvents {
    private AudioClientEvents() { }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) AudioClient.instance().tick();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { AudioClient.instance().disconnect(); }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) { AudioClientCommands.register(event.getDispatcher()); }
}
