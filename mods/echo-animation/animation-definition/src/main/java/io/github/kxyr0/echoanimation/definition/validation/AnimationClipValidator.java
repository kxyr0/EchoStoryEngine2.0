package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates visual clip tracks against an immutable rig.
 */
public final class AnimationClipValidator {
    public void validate(AnimationRigDefinition rig, AnimationClipDefinition clip) {
        clip.rigId().ifPresent(id -> {
            if (!id.equals(rig.id())) {
                throw new IllegalArgumentException("Clip rig mismatch: " + clip.id());
            }
        });
        Set<BoneId> bones = rig.bones().stream().map(bone -> bone.id()).collect(Collectors.toUnmodifiableSet());
        Set<BoneId> tracks = new HashSet<>();
        for (var track : clip.tracks()) {
            if (!bones.contains(track.boneId())) {
                throw new IllegalArgumentException("Unknown bone track: " + track.boneId());
            }
            if (!tracks.add(track.boneId())) {
                throw new IllegalArgumentException("Duplicate bone track: " + track.boneId());
            }
        }
    }
}
