package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationBoneGeometry;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Validates immutable body rig definitions before catalog apply. */
public final class AnimationRigValidator {
    public void validate(AnimationRigDefinition rig) {
        Map<BoneId, AnimationBoneDefinition> byId = rig.bones().stream()
                        .collect(Collectors.toMap(AnimationBoneDefinition::id, bone -> bone));
        if (byId.size() != rig.bones().size()) {
            throw new IllegalArgumentException("Duplicate bone id in rig: " + rig.id());
        }
        requireParents(rig.bones(), byId);
        requireNoCycles(rig.bones(), byId);
        requireGeometry(rig.geometry());
        requireSockets(rig, byId.keySet());
        if (rig.kind() == AnimationRigKind.SEGMENTED_HUMANOID) {
            requireSegmentedHumanoid(rig.bones(), byId);
        }
    }

    private void requireParents( List<AnimationBoneDefinition> bones, Map<BoneId, AnimationBoneDefinition> byId) {
        for (AnimationBoneDefinition bone : bones) {
            bone.parentId() .ifPresent( parent -> {
                                if (!byId.containsKey(parent)) {
                                    throw new IllegalArgumentException( "Parent bone is missing: " + parent);
                                }
                            });
        }
    }

    private void requireNoCycles( List<AnimationBoneDefinition> bones, Map<BoneId, AnimationBoneDefinition> byId) {
        for (AnimationBoneDefinition bone : bones) {
            Set<BoneId> seen = new HashSet<>();
            AnimationBoneDefinition current = bone;
            while (current.parentId().isPresent()) {
                BoneId parent = current.parentId().orElseThrow();
                if (!seen.add(parent)) {
                    throw new IllegalArgumentException( "Bone hierarchy contains cycle: " + bone.id());
                }
                current = byId.get(parent);
            }
        }
    }

    private void requireGeometry(List<AnimationBoneGeometry> geometry) {
        Set<String> ids = new HashSet<>();
        Set<BoneId> baseOwners = new HashSet<>();
        for (AnimationBoneGeometry entry : geometry) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate geometry id: " + entry.id());
            }
            if (!entry.overlay() && !baseOwners.add(entry.ownerBoneId())) {
                throw new IllegalArgumentException( "Duplicate base geometry owner: " + entry.ownerBoneId());
            }
        }
    }

    private void requireSockets(AnimationRigDefinition rig, Set<BoneId> boneIds) {
        Set<String> sockets = new HashSet<>();
        rig.sockets() .forEach( socket -> {
                            if (!sockets.add(socket.id().toString())) {
                                throw new IllegalArgumentException( "Duplicate socket id: " + socket.id());
                            }
                            if (!boneIds.contains(socket.parentBoneId())) {
                                throw new IllegalArgumentException(
                                        "Socket parent bone is missing: " + socket.parentBoneId());
                            }
                        });
    }

    private void requireSegmentedHumanoid(
            List<AnimationBoneDefinition> bones, Map<BoneId, AnimationBoneDefinition> byId) {
        if (!byId.keySet().equals(SegmentedHumanoidBone.ids())) {
            throw new IllegalArgumentException(
                    "Segmented humanoid rig must contain exactly canonical split-torso bones.");
        }
        for (SegmentedHumanoidBone expected : SegmentedHumanoidBone.values()) {
            if (!byId.get(expected.id()).parentId().equals(expected.parentId())) {
                throw new IllegalArgumentException( "Segmented humanoid parent mismatch: " + expected.id());
            }
        }
    }
}
