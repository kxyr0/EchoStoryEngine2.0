package io.github.kxyr0.echonpc.forge.api.client.attachment;

import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;

/**
 * Renders an attachment against generic NPC body anchors.
 */
@FunctionalInterface
public interface NpcBodyAttachmentRenderExtension {
    NpcClientRenderResult render(NpcBodyAttachmentRenderContext context);
}
