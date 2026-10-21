package io.github.kxyr0.echoitemattachments.npc;

import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderRegistrationEvent;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionId;

final class NpcClientRegistration {
    private NpcClientRegistration() {
    }

    static void register() {
        NpcBodyAttachmentRenderRegistrationEvent.registerGlobal(
                new NpcClientRenderExtensionDescriptor(
                        new NpcClientRenderExtensionId("echo_item_attachments:hand_items"),
                        "Echo real hand items"),
                NpcHandItemRenderer::new);
    }
}
