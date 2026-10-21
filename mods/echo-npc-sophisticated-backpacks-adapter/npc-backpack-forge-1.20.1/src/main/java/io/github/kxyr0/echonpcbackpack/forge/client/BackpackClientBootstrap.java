package io.github.kxyr0.echonpcbackpack.forge.client;

import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderRegistrationEvent;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionId;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class BackpackClientBootstrap {
    private static final BackpackTransformCatalog TRANSFORMS = new BackpackTransformCatalog();

    private BackpackClientBootstrap() {
    }

    public static void register() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        NpcBodyAttachmentRenderRegistrationEvent.registerGlobal(
                new NpcClientRenderExtensionDescriptor(
                        new NpcClientRenderExtensionId("echo_npc_sophisticated_backpacks:backpack"),
                        "Sophisticated Backpack"),
                () -> new BackpackAttachmentExtension(TRANSFORMS));
        modBus.addListener(BackpackClientBootstrap::reloadListeners);
        MinecraftForge.EVENT_BUS.addListener(BackpackClientBootstrap::logout);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(TRANSFORMS);
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        BackpackClientState.clear();
    }
}
