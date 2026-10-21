package io.github.kxyr0.echoitemattachments.player;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class PlayerClientRegistration {
    private PlayerClientRegistration() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(PlayerFirstPersonRenderer::render);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PlayerThirdPersonLayer::addLayers);
    }
}
