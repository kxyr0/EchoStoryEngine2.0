package io.github.kxyr0.echoplayersleep.script.forge;

import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceLookupEvent;
import io.github.kxyr0.echoplayersleep.script.SleepActionDelayScheduler;
import io.github.kxyr0.echoplayersleep.script.SleepStoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoPlayerSleepScriptAdapterMod.MOD_ID)
public final class EchoPlayerSleepScriptAdapterMod {
    public static final String MOD_ID = "echo_player_sleep_script_adapter";

    public EchoPlayerSleepScriptAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onStoryExtensionRegistration);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    private void onStoryExtensionRegistration(EchoStoryExtensionRegistrationEvent event) {
        PlayerSleepServiceLookupEvent lookup = new PlayerSleepServiceLookupEvent(event.server());
        MinecraftForge.EVENT_BUS.post(lookup);
        lookup.bridge().ifPresent(bridge -> event.registrar().register(new ForgeStoryExtensionFactory() {
            @Override
            public io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor descriptor() {
                return new SleepStoryExtensionSession(bridge, "0.1.0").descriptor();
            }

            @Override
            public io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession create(
                    io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext context) {
                return new SleepStoryExtensionSession(bridge, "0.1.0");
            }
        }));
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SleepActionDelayScheduler.tick();
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        SleepActionDelayScheduler.clear();
    }
}
