package io.github.kxyr0.echoik.core.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable skeleton graph. The graph can represent any rig shape.
 */
public final class SkeletonDefinition {
    private final Map<String, Bone> bones;

    public SkeletonDefinition(List<Bone> bones) {
        Objects.requireNonNull(bones, "bones");
        Map<String, Bone> values = new LinkedHashMap<>();
        for (Bone bone : bones) {
            if (values.putIfAbsent(bone.id(), bone) != null) {
                throw new IllegalArgumentException("Duplicate bone id: " + bone.id());
            }
        }
        for (Bone bone : values.values()) {
            bone.parentId().ifPresent(parent -> requireParent(values, bone, parent));
        }
        this.bones = Map.copyOf(values);
    }

    public Optional<Bone> bone(String id) {
        return Optional.ofNullable(bones.get(id));
    }

    public Bone requireBone(String id) {
        return bone(id).orElseThrow(() -> new IllegalArgumentException("Unknown bone id: " + id));
    }

    public double boneLength(String id, double fallback) {
        return bone(id).map(Bone::length).filter(length -> length > 0.0D).orElse(fallback);
    }

    public List<Bone> bones() {
        return List.copyOf(bones.values());
    }

    private static void requireParent(Map<String, Bone> values, Bone bone, String parent) {
        if (!values.containsKey(parent)) {
            throw new IllegalArgumentException("Bone " + bone.id() + " references missing parent: " + parent);
        }
    }
}
