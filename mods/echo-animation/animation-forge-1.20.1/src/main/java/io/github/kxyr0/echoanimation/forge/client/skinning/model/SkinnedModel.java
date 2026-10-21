package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import java.util.List;
import java.util.Objects;

/** Validated v1/v2 skinned model loaded from a resource pack. */
public record SkinnedModel(
        int formatVersion,
        String source,
        List<SkinnedBone> bones,
        List<SkinnedMesh> meshes,
        float renderOffsetX,
        float renderOffsetY,
        float renderOffsetZ,
        SkinnedAttachmentBindings attachmentBindings
) {
    public SkinnedModel {
        source = Objects.requireNonNull(source, "source");
        bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        meshes = List.copyOf(Objects.requireNonNull(meshes, "meshes"));
        attachmentBindings = Objects.requireNonNull(attachmentBindings, "attachmentBindings");
    }

    public SkinnedModel(
            int formatVersion,
            String source,
            List<SkinnedBone> bones,
            List<SkinnedMesh> meshes
    ) {
        this(
                formatVersion,
                source,
                bones,
                meshes,
                0.0F,
                0.0F,
                0.0F,
                SkinnedAttachmentBindings.legacy());
    }

    public boolean hasRenderOffset() {
        return renderOffsetX != 0.0F
                || renderOffsetY != 0.0F
                || renderOffsetZ != 0.0F;
    }

    public int vertexCount() {
        return meshes.stream()
                .mapToInt(mesh -> mesh.vertices().size())
                .sum();
    }

    public int triangleVertexCount() {
        return meshes.stream()
                .mapToInt(mesh -> mesh.triangles().length)
                .sum();
    }
}
