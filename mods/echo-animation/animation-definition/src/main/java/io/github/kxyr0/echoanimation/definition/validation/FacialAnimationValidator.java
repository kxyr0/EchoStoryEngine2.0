package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates animation tracks against a rig.
 */
public final class FacialAnimationValidator {
    public void validate(FaceRigDefinition rig, FacialAnimationDefinition animation) {
        Set<BoneId> bones = rig.hierarchy().bones().stream().map(bone -> bone.id()).collect(Collectors.toSet());
        validateTracks(bones, animation.tracks());
        for (var surface : animation.surfaces()) {
            validateTracks(bones, surface.enter().tracks());
            validateTracks(bones, surface.stay().tracks());
            validateTracks(bones, surface.exit().tracks());
        }
    }

    private void validateTracks(Set<BoneId> bones, List<BoneTrack> source) {
        Set<BoneId> tracks = new LinkedHashSet<>();
        for (var track : source) {
            if (!bones.contains(track.boneId())) {
                throw new IllegalArgumentException("Animation track bone не найден: " + track.boneId());
            }
            if (!tracks.add(track.boneId())) {
                throw new IllegalArgumentException("Animation track объявлен повторно: " + track.boneId());
            }
        }
    }
}
