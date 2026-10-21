package io.github.kxyr0.echoitemattachments.forge.client;

import io.github.kxyr0.echoitemattachments.forge.api.client.ItemGripCatalog;
import io.github.kxyr0.echoitemattachments.forge.network.ClientAttachmentStore;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EchoItemAttachmentsClient {
    private EchoItemAttachmentsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EchoItemAttachmentsClient::reloadListeners);
        MinecraftForge.EVENT_BUS.addListener(EchoItemAttachmentsClient::logout);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ItemGripCatalog.INSTANCE);
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAttachmentStore.clear();
    }
}
