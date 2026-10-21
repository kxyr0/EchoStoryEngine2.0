package io.github.kxyr0.echoanimation.forge.client.humanoid.state;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.sync.AnimationClientSyncState;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Composes one foreground BODY playback over a non-owning idle base layer. */
final class HumanoidPoseLayers {
    private static final BoneId BODY_LOWER = new BoneId("body_lower");
    private static final BoneId LEGACY_BODY = new BoneId("body");

    private HumanoidPoseLayers() {
    }

    static Set<BoneId> authoredBones(
            AnimationHumanoidRenderContext context,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips
    ) {
        Set<BoneId> bones = new LinkedHashSet<>();
        var snapshot = AnimationClientSyncState.current(context.subjectId());
        if (snapshot.isEmpty()) {
            return bones;
        }
        for (var channel : snapshot.orElseThrow().channels()) {
            channel.previousClipId().ifPresent(id -> addAuthoredBones(bones, id, rig, clips));
            channel.clipId().ifPresent(id -> addAuthoredBones(bones, id, rig, clips));
        }
        return bones;
    }

    static AnimationPose overlay(AnimationPose base, AnimationPose foreground, Set<BoneId> authored) {
        Map<BoneId, EulerTransform> euler = new LinkedHashMap<>(base.transforms());
        Map<BoneId, Matrix4Transform> matrices = new LinkedHashMap<>(base.matrixTransforms());
        for (BoneId boneId : authored) {
            foreground.transform(boneId).ifPresent(value -> {
                euler.put(boneId, value);
                matrices.remove(boneId);
            });
            foreground.matrixTransform(boneId).ifPresent(value -> {
                matrices.put(boneId, value);
                euler.remove(boneId);
            });
        }
        return AnimationPose.of(euler, matrices);
    }

    private static void addAuthoredBones(
            Set<BoneId> target,
            AnimationClipId clipId,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips
    ) {
        compatibleClip(clipId, rig, clips).ifPresent(clip -> {
            boolean splitBody = rig.bones().stream().anyMatch(bone -> bone.id().equals(BODY_LOWER));
            for (var track : clip.tracks()) {
                BoneId boneId = track.boneId();
                if (splitBody && clip.schemaVersion() < 3 && boneId.equals(LEGACY_BODY)) {
                    target.add(BODY_LOWER);
                } else {
                    target.add(boneId);
                }
            }
        });
    }

    private static Optional<AnimationClipDefinition> compatibleClip(
            AnimationClipId clipId,
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips
    ) {
        return clips.stream()
                .filter(clip -> clip.id().equals(clipId))
                .filter(clip -> clip.rigId().map(rig.id()::equals).orElse(true))
                .findFirst();
    }
}
