package io.github.kxyr0.echoanimationauthoring;

import io.github.kxyr0.echoanimationauthoring.client.AuthoringClientEvents;
import io.github.kxyr0.echoanimationauthoring.editor.AuthoringLabels;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Client-only entrypoint for development authoring tools.
 */
@Mod(EchoAnimationAuthoringMod.MOD_ID)
public final class EchoAnimationAuthoringMod {
    public static final String MOD_ID = "echo_animation_authoring";

    public EchoAnimationAuthoringMod(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            throw new IllegalStateException("Echo Animation Authoring устанавливается только в client dev instance.");
        }
        context.getModEventBus().addListener(this::onClientSetup);
        AuthoringClientEvents.register();
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        AuthoringLabels.requireRussianLabels();
    }
}
