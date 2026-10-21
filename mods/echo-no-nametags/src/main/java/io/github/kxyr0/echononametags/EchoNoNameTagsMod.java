package io.github.kxyr0.echononametags;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoNoNameTagsMod.MOD_ID)
public final class EchoNoNameTagsMod {
    public static final String MOD_ID = "echo_no_nametags";

    public EchoNoNameTagsMod() {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onRenderNameTag(RenderNameTagEvent event) {
            event.setResult(Event.Result.DENY);
        }
    }
}
