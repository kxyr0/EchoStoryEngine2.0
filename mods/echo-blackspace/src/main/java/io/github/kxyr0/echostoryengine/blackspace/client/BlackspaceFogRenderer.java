package io.github.kxyr0.echostoryengine.blackspace.client;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import io.github.kxyr0.echostoryengine.blackspace.shader.BlackspaceShaderController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoBlackspace.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackspaceFogRenderer {
    private static final float FOG_NEAR = 1_000_000.0F;
    private static final float FOG_FAR = 1_000_001.0F;
    private static final float FOG_RED = 0.0F;
    private static final float FOG_GREEN = 0.0F;
    private static final float FOG_BLUE = 0.0F;

    private BlackspaceFogRenderer() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!BlackspaceClientState.isActive() || !BlackspaceShaderController.useForgeVisualFallback()) {
            return;
        }
        event.setRed(FOG_RED);
        event.setGreen(FOG_GREEN);
        event.setBlue(FOG_BLUE);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!BlackspaceClientState.isActive() || !BlackspaceShaderController.useForgeVisualFallback()) {
            return;
        }
        event.setNearPlaneDistance(FOG_NEAR);
        event.setFarPlaneDistance(FOG_FAR);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
