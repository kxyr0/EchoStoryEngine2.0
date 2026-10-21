package io.github.kxyr0.echoeventsintegration;

import io.github.kxyr0.echoeventsintegration.action.TriggerActions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_events_integration")
public final class EchoEventsIntegrationMod {
    public EchoEventsIntegrationMod() {
        MinecraftForge.EVENT_BUS.register(new TriggerActions());
        MinecraftForge.EVENT_BUS.register(new io.github.kxyr0.echoeventsintegration.producer.AnimationEventAdapter());
        MinecraftForge.EVENT_BUS.register(new io.github.kxyr0.echoeventsintegration.producer.NpcEventAdapter());
    }
}
