package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;

import java.util.Objects;

/** Immutable position, normal, UV and four skin influences. */
public record SkinnedVertex(
        float x, float y, float z,
        float normalX, float normalY, float normalZ,
        float u, float v,
        SkinWeights skin
) {
    public SkinnedVertex {
        skin = Objects.requireNonNull(skin, "skin");
        float[] values = {x, y, z, normalX, normalY, normalZ, u, v};
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Skinned vertex values must be finite.");
            }
        }
    }
}
