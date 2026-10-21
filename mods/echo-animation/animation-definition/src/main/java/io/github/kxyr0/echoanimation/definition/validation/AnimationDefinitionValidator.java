package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Validates high-level lifecycle definitions against known visual clips. */
public final class AnimationDefinitionValidator {
    private static final double TOLERANCE = 0.0001D;
    private final NormalizedClipSampler sampler = new NormalizedClipSampler();

    public void validate( AnimationDefinition definition, AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips) {
        Map<AnimationClipId, AnimationClipDefinition> byId = clips.stream() .collect( Collectors.toMap(
                                        AnimationClipDefinition::id, Function.identity(), (left, right) -> left));
        requireEntries(definition);
        requireExitPoints(definition);
        ClipSet clipSet = clips(definition, byId);
        requireCoverage(clipSet);
        requireContinuity(definition, rig, clipSet);
    }

    private void requireEntries(AnimationDefinition definition) {
        // Entry clips are optional. A direct-main animation (idle/walk/run,
        // one-shot or hold) may intentionally start in MAIN without authored
        // ENTER boilerplate. AnimationPlaybackStartFlow already supports this:
        // an empty entry list starts MAIN immediately from any allowed source.
        if (definition.entryVariants().isEmpty()) {
            return;
        }

        Set<Object> sources = new HashSet<>();
        for (AnimationDefinition.EntryVariant entry : definition.entryVariants()) {
            if (!sources.add(entry.sourceState())) {
                throw new IllegalArgumentException( "Duplicate source entry variant: " + entry.sourceState());
            }
        }
        if (!sources.equals(definition.allowedSourceStates())) {
            throw new IllegalArgumentException(
                    "Entry variants must exactly match allowed source states when entries are" + " authored.");
        }
    }

    private void requireExitPoints(AnimationDefinition definition) {
        Set<String> points = new HashSet<>();
        for (AnimationDefinition.ExitPoint point : definition.main().exitPoints()) {
            if (!points.add(point.id())) {
                throw new IllegalArgumentException("Duplicate exit point: " + point.id());
            }
        }
        for (AnimationDefinition.ExitVariant exit : definition.exitVariants()) {
            if (!points.contains(exit.exitPointId())) {
                throw new IllegalArgumentException(
                        "Exit variant references missing exit point: " + exit.exitPointId());
            }
        }
        if (!points.equals( definition.exitVariants().stream() .map(AnimationDefinition.ExitVariant::exitPointId)
                        .collect(Collectors.toSet()))) {
            throw new IllegalArgumentException( "Every exit point requires exactly one exit variant.");
        }
    }

    private ClipSet clips( AnimationDefinition definition, Map<AnimationClipId, AnimationClipDefinition> byId) {
        List<AnimationClipDefinition> entries = definition.entryVariants().stream()
                        .map(entry -> clip(byId, entry.clipId())) .toList();
        AnimationClipDefinition main = clip(byId, definition.main().clipId());
        List<AnimationClipDefinition> exits =
                definition.exitVariants().stream().map(exit -> clip(byId, exit.clipId())).toList();
        return new ClipSet(entries, main, exits);
    }

    private AnimationClipDefinition clip( Map<AnimationClipId, AnimationClipDefinition> byId, AnimationClipId id) {
        AnimationClipDefinition clip = byId.get(id);
        if (clip == null) {
            throw new IllegalArgumentException("Unknown animation clip: " + id);
        }
        return clip;
    }

    private void requireCoverage(ClipSet clipSet) {
        Set<BoneId> mainBones = bones(clipSet.main());
        for (AnimationClipDefinition entry : clipSet.entries()) {
            if (!bones(entry).containsAll(mainBones)) {
                throw new IllegalArgumentException("MISSING_ENTRY_BONE_COVERAGE");
            }
        }
        for (AnimationClipDefinition exit : clipSet.exits()) {
            if (!bones(exit).containsAll(mainBones)) {
                throw new IllegalArgumentException("MISSING_EXIT_BONE_COVERAGE");
            }
        }
    }

    private Set<BoneId> bones(AnimationClipDefinition clip) {
        return clip.tracks().stream() .map(track -> track.boneId()) .collect(Collectors.toUnmodifiableSet());
    }

    private void requireContinuity( AnimationDefinition definition, AnimationRigDefinition rig, ClipSet clipSet) {
        AnimationPose base = AnimationPose.defaults(rig.bones());
        Set<BoneId> mainBones = bones(clipSet.main());
        AnimationPose mainStart = sampler.sample( clipSet.main(), base, 0.0D, definition.main().segmentId(),
                        definition.main().pointId());
        for (int index = 0; index < clipSet.entries().size(); index++) {
            AnimationDefinition.EntryVariant phase = definition.entryVariants().get(index);
            requireSame( sampler.sample( clipSet.entries().get(index), base, 1.0D, phase.segmentId(), Optional.empty()),
                    mainStart, mainBones, "ANIMATION_PHASE_POSE_MISMATCH");
        }
        for (int index = 0; index < definition.exitVariants().size(); index++) {
            double mainProgress = mainProgress(definition, definition.exitVariants().get(index).exitPointId());
            AnimationPose mainPose = sampler.sample( clipSet.main(), base, mainProgress, definition.main().segmentId(),
                            definition.main().pointId());
            AnimationDefinition.ExitVariant phase = definition.exitVariants().get(index);
            requireSame( mainPose, sampler.sample( clipSet.exits().get(index), base, 0.0D, phase.segmentId(),
                            Optional.empty()), mainBones, "ANIMATION_PHASE_POSE_MISMATCH");
            requireSame( sampler.sample( clipSet.exits().get(index), base, 1.0D, phase.segmentId(), Optional.empty()),
                    base, mainBones, "EXIT_TARGET_POSE_MISMATCH");
        }
    }

    private double mainProgress(AnimationDefinition definition, String exitPointId) {
        if (definition.main().mode() == MainMode.ONCE) {
            return 1.0D;
        }
        return definition.main().exitPoints().stream() .filter(point -> point.id().equals(exitPointId)) .findFirst()
                .orElseThrow() .progress();
    }

    private void requireSame( AnimationPose left, AnimationPose right, Set<BoneId> bones, String code) {
        for (BoneId bone : bones) {
            if (!same( left.transform(bone).orElse(EulerTransform.IDENTITY),
                    right.transform(bone).orElse(EulerTransform.IDENTITY))) {
                throw new IllegalArgumentException(code + ": " + bone);
            }
        }
    }

    private boolean same(EulerTransform left, EulerTransform right) {
        return same(left.translation(), right.translation()) && same(left.rotationDegrees(), right.rotationDegrees())
                && same(left.scale(), right.scale());
    }

    private boolean same(Vec3 left, Vec3 right) {
        return Math.abs(left.x() - right.x()) <= TOLERANCE && Math.abs(left.y() - right.y()) <= TOLERANCE
                && Math.abs(left.z() - right.z()) <= TOLERANCE;
    }

    private record ClipSet( List<AnimationClipDefinition> entries, AnimationClipDefinition main,
            List<AnimationClipDefinition> exits) {}
}
