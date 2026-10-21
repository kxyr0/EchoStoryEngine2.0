package io.github.kxyr0.echonpc.forge.api.client.attachment;

@FunctionalInterface
public interface NpcReplacementAttachmentRenderer {

    NpcReplacementAttachmentRenderer NONE =
            anchors -> {
            };

    void render(
            NpcVisualAttachmentTransformProvider anchors);
}
