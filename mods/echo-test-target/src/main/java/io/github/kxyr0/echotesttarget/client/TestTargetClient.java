package io.github.kxyr0.echotesttarget.client;

import io.github.kxyr0.echotesttarget.EchoTestTargetMod;
import io.github.kxyr0.echotesttarget.registry.TestTargetEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoTestTargetMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TestTargetClient {
    private TestTargetClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TestTargetEntities.IMMORTAL_TARGET.get(), ImmortalTestTargetRenderer::new);
    }
}

