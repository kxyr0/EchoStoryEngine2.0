package io.github.kxyr0.echobackgroundnpc.forge.client;

import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class BackgroundClientSetup {
    private BackgroundClientSetup() { }
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BackgroundNpcEntities.BACKGROUND_NPC.get(), BackgroundNpcRenderer::new);
    }
}
