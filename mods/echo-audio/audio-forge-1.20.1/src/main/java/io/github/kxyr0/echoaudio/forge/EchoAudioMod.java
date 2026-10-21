package io.github.kxyr0.echoaudio.forge;

import io.github.kxyr0.echoaudio.forge.network.AudioPackets;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import io.github.kxyr0.echoaudio.forge.story.AudioStoryFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.lifecycle.EchoStoryExecutionTerminalEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_audio")
public final class EchoAudioMod {
    public EchoAudioMod() { AudioPackets.register(); MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new io.github.kxyr0.echoaudio.forge.dialogue.AudioDialogueListener()); }
    @SubscribeEvent public void registerStory(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new AudioStoryFactory());
    }
    @SubscribeEvent public void terminal(EchoStoryExecutionTerminalEvent event) {
        AudioServer.of(event.server()).cancelOwned("story:" + event.executionId());
    }
    @SubscribeEvent public void stopping(ServerStoppingEvent event) { AudioServer.close(event.getServer()); }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            AudioServer.of(player.getServer()).disconnected(player);
        }
    }
}
