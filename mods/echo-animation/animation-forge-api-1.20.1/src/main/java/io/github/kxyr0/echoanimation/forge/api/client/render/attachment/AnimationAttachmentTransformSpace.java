package io.github.kxyr0.echoanimation.forge.api.client.render.attachment;

/** Describes how an attachment transform must be composed by downstream renderers. */
public enum AnimationAttachmentTransformSpace {
    /** Historical rig transform that still needs the vanilla body-part pivot. */
    LEGACY_PART_RELATIVE,

    /** Complete model-space socket transform. No vanilla body-part pivot may be appended. */
    MODEL_ABSOLUTE
}
