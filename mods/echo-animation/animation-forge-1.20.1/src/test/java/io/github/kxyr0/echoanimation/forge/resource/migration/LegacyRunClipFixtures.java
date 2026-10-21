package io.github.kxyr0.echoanimation.forge.resource.migration;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Original three-file run poses retained only as a migration oracle.
 */
final class LegacyRunClipFixtures {
    private LegacyRunClipFixtures() {
    }

    static AnimationClipDefinition loop() {
        List<AnimationBoneTrackDefinition> tracks = new ArrayList<>();
        tracks.add(track("body", List.of(
                frame(0.0D, 11.7D, -8.0D, InterpolationKind.LINEAR),
                frame(0.25D, 11.3D, -10.0D, InterpolationKind.CUBIC),
                frame(0.5D, 11.7D, -8.0D, InterpolationKind.CUBIC),
                frame(0.75D, 11.3D, -10.0D, InterpolationKind.CUBIC),
                frame(1.0D, 11.7D, -8.0D, InterpolationKind.LINEAR))));
        tracks.add(rotationTrack("head", 8.0D, 8.0D));
        valuesA().forEach((bone, value) -> tracks.add(rotationTrack(bone, value, valuesB().get(bone))));
        return AnimationClipDefinition.legacy(AnimationClipId.parse("jdh:legacy_run_loop"), Optional.empty(),
                tracks, new AnimationClipDefinition.LegacySettings(2, AnimationLoopSeam.SMOOTH, 4.3D, "legacy-loop"));
    }

    static AnimationClipDefinition enter() {
        return phase("jdh:legacy_run_enter", true);
    }

    static AnimationClipDefinition exit() {
        return phase("jdh:legacy_run_exit", false);
    }

    private static AnimationClipDefinition phase(String id, boolean enter) {
        List<AnimationBoneTrackDefinition> tracks = new ArrayList<>();
        tracks.add(track("body", List.of(
                frame(0.0D, enter ? 12.0D : 11.7D, enter ? 0.0D : -8.0D, InterpolationKind.LINEAR),
                frame(1.0D, enter ? 11.7D : 12.0D, enter ? -8.0D : 0.0D, InterpolationKind.LINEAR))));
        tracks.add(phaseRotation("head", 8.0D, enter));
        valuesA().forEach((bone, value) -> tracks.add(phaseRotation(bone, value, enter)));
        return new AnimationClipDefinition(AnimationClipId.parse(id), Optional.empty(), tracks, id);
    }

    private static AnimationBoneTrackDefinition rotationTrack(String bone, double first, double middle) {
        return track(bone, List.of(rotation(0.0D, first, InterpolationKind.CUBIC),
                rotation(0.5D, middle, InterpolationKind.CUBIC),
                rotation(1.0D, first, InterpolationKind.LINEAR)));
    }

    private static AnimationBoneTrackDefinition phaseRotation(String bone, double authored, boolean enter) {
        return track(bone, List.of(rotation(0.0D, enter ? 0.0D : authored, InterpolationKind.LINEAR),
                rotation(1.0D, enter ? authored : 0.0D, InterpolationKind.LINEAR)));
    }

    private static AnimationBoneTrackDefinition track(String bone, List<AnimationKeyframeDefinition> frames) {
        return new AnimationBoneTrackDefinition(BoneId.parse(bone), frames);
    }

    private static AnimationKeyframeDefinition rotation(double progress, double x, InterpolationKind interpolation) {
        return new AnimationKeyframeDefinition(progress, Optional.empty(), Optional.of(new Vec3(x, 0.0D, 0.0D)),
                Optional.empty(), interpolation);
    }

    private static AnimationKeyframeDefinition frame(double progress, double y, double x,
            InterpolationKind interpolation) {
        return new AnimationKeyframeDefinition(progress, Optional.of(new Vec3(0.0D, y, 0.0D)),
                Optional.of(new Vec3(x, 0.0D, 0.0D)), Optional.empty(), interpolation);
    }

    private static Map<String, Double> valuesA() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("left_upper_arm", -48.0D);
        values.put("left_forearm", -28.0D);
        values.put("right_upper_arm", 48.0D);
        values.put("right_forearm", -12.0D);
        values.put("left_thigh", 52.0D);
        values.put("left_shin", 10.0D);
        values.put("right_thigh", -52.0D);
        values.put("right_shin", 38.0D);
        return values;
    }

    private static Map<String, Double> valuesB() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("left_upper_arm", 48.0D);
        values.put("left_forearm", -12.0D);
        values.put("right_upper_arm", -48.0D);
        values.put("right_forearm", -28.0D);
        values.put("left_thigh", -52.0D);
        values.put("left_shin", 38.0D);
        values.put("right_thigh", 52.0D);
        values.put("right_shin", 10.0D);
        return values;
    }
}
