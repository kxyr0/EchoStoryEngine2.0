package io.github.kxyr0.echonpc.forge.api.client.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;

@FunctionalInterface
public interface NpcVisualAttachmentTransformProvider {

    void apply(
            NpcVisualAttachmentAnchor anchor,
            PoseStack poses);

    /**
     * Tells the NPC renderer whether it must append the vanilla body-part pivot.
     * Existing lambda providers stay part-relative for compatibility.
     */
    default NpcVisualAttachmentTransformSpace space(
            NpcVisualAttachmentAnchor anchor) {

        Objects.requireNonNull(anchor, "anchor");
        return NpcVisualAttachmentTransformSpace.PART_RELATIVE;
    }
}
