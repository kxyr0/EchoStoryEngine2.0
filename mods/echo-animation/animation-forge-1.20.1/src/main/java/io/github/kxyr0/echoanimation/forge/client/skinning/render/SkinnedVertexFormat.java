package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/** Interleaved GPU layout: position, UV, ubyte bone indices, weights and normal. */
final class SkinnedVertexFormat {
    private static final VertexFormatElement BONE_INDICES = new VertexFormatElement(1,
            VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.UV, 4);
    private static final VertexFormatElement BONE_WEIGHTS = new VertexFormatElement(2,
            VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4);

    static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
            .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
            .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
            .put("BoneIndices", BONE_INDICES)
            .put("BoneWeights", BONE_WEIGHTS)
            .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
            .put("Padding", DefaultVertexFormat.ELEMENT_PADDING)
            .build());

    private SkinnedVertexFormat() {
    }
}
