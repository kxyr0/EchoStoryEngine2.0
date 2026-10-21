package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import java.util.List;
import java.util.Objects;

/** One triangulated mesh using the model's shared skeleton and skin texture. */
public record SkinnedMesh(String name, List<SkinnedVertex> vertices, int[] triangles) {
    public SkinnedMesh {
        name = Objects.requireNonNull(name, "name");
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        triangles = Objects.requireNonNull(triangles, "triangles").clone();
    }

    @Override
    public int[] triangles() {
        return triangles.clone();
    }
}
