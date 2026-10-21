package io.github.kxyr0.echonpc.forge.api.client.attachment;

/** Composition contract for a visual attachment anchor transform. */
public enum NpcVisualAttachmentTransformSpace {
    /** Historical transform that still requires the vanilla ModelPart pivot. */
    PART_RELATIVE,

    /** Complete model-space socket transform. No vanilla ModelPart pivot may be appended. */
    MODEL_ABSOLUTE
}
