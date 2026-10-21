package io.github.kxyr0.echoplayeractor.story;

import io.github.kxyr0.echoplayeractor.story.bridge.PlayerActorStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoStoryPlayerActorAdapterMod.MOD_ID)
public final class EchoStoryPlayerActorAdapterMod {
    public static final String MOD_ID = "echo_story_player_actor_adapter";

    public EchoStoryPlayerActorAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new PlayerActorStoryExtensionFactory());
    }
}
