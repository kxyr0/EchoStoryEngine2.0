package io.github.kxyr0.echoanimation.core.hierarchy;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic validated bone hierarchy.
 */
public final class BoneHierarchy {
    private final List<BoneDefinition> bones;
    private final Map<BoneId, BoneDefinition> byId;

    public BoneHierarchy(List<BoneDefinition> bones) {
        this.bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        this.byId = index(this.bones);
        new BoneHierarchyValidator(32).validate(this.bones);
    }

    public List<BoneDefinition> bones() {
        return bones;
    }

    public Optional<BoneDefinition> find(BoneId id) {
        return Optional.ofNullable(byId.get(id));
    }

    private static Map<BoneId, BoneDefinition> index(List<BoneDefinition> source) {
        Map<BoneId, BoneDefinition> result = new LinkedHashMap<>();
        for (BoneDefinition bone : source) {
            if (result.putIfAbsent(bone.id(), bone) != null) {
                throw new IllegalArgumentException("BoneId объявлен повторно: " + bone.id());
            }
        }
        return Map.copyOf(result);
    }
}

