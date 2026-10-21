package io.github.kxyr0.echoanimation.core.hierarchy;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates parent references, cycles and maximum depth.
 */
public final class BoneHierarchyValidator {
    private final int maxDepth;

    public BoneHierarchyValidator(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth должен быть положительным.");
        }
        this.maxDepth = maxDepth;
    }

    public void validate(List<BoneDefinition> bones) {
        Objects.requireNonNull(bones, "bones");
        Map<BoneId, BoneDefinition> byId = bones.stream().collect(Collectors.toMap(BoneDefinition::id, bone -> bone));
        for (BoneDefinition bone : bones) {
            validateParentExists(bone, byId);
            validateAncestry(bone, byId);
        }
    }

    private void validateParentExists(BoneDefinition bone, Map<BoneId, BoneDefinition> byId) {
        bone.parentId().ifPresent(parent -> {
            if (!byId.containsKey(parent)) {
                throw new IllegalArgumentException("Parent bone не найден: " + parent);
            }
        });
    }

    private void validateAncestry(BoneDefinition bone, Map<BoneId, BoneDefinition> byId) {
        Set<BoneId> seen = new HashSet<>();
        BoneDefinition current = bone;
        int depth = 0;
        while (current.parentId().isPresent()) {
            if (++depth > maxDepth) {
                throw new IllegalArgumentException("Bone hierarchy depth превышен: " + bone.id());
            }
            BoneId parent = current.parentId().orElseThrow();
            if (!seen.add(parent)) {
                throw new IllegalArgumentException("Bone hierarchy содержит cycle: " + bone.id());
            }
            current = byId.get(parent);
        }
    }
}

