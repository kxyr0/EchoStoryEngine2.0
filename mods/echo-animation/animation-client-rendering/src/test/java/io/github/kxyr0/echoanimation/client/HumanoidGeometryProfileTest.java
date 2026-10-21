package io.github.kxyr0.echoanimation.client;

import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import io.github.kxyr0.echoanimation.client.humanoid.HumanoidSegmentId;
import io.github.kxyr0.echoanimation.client.humanoid.SegmentedHumanoidGeometryBuilder;
import io.github.kxyr0.echoanimation.client.humanoid.SegmentedHumanoidGeometryProfile;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HumanoidGeometryProfileTest {
    private final SegmentedHumanoidGeometryBuilder builder = new SegmentedHumanoidGeometryBuilder();

    @Test
    void canonicalBonesContainNoForbiddenPlaceholders() {
        Set<String> ids = SegmentedHumanoidBone.ids().stream().map(Object::toString).collect(Collectors.toSet());
        Assertions.assertFalse(ids.contains("left_hand"));
        Assertions.assertFalse(ids.contains("right_hand"));
        Assertions.assertFalse(ids.contains("left_foot"));
        Assertions.assertFalse(ids.contains("right_foot"));
        Assertions.assertFalse(ids.contains("mouth"));
    }

    @Test
    void classicAndSlimArmWidthsMatchVanillaVariants() {
        Assertions.assertEquals(4.0D, profile(AnimationSkinVariant.CLASSIC)
                .geometry(HumanoidSegmentId.RIGHT_UPPER_ARM, false).orElseThrow().widthPixels(), 0.0001D);
        Assertions.assertEquals(3.0D, profile(AnimationSkinVariant.SLIM)
                .geometry(HumanoidSegmentId.RIGHT_UPPER_ARM, false).orElseThrow().widthPixels(), 0.0001D);
    }

    @Test
    void armsUseFivePixelUpperAndSevenPixelForearmSegments() {
        SegmentedHumanoidGeometryProfile profile = profile(AnimationSkinVariant.CLASSIC);
        Assertions.assertEquals(5.0D,
                profile.geometry(HumanoidSegmentId.RIGHT_UPPER_ARM, false).orElseThrow().heightPixels(), 0.0001D);
        Assertions.assertEquals(7.0D,
                profile.geometry(HumanoidSegmentId.RIGHT_FOREARM, false).orElseThrow().heightPixels(), 0.0001D);
        Assertions.assertEquals(5.0D,
                profile.geometry(HumanoidSegmentId.LEFT_UPPER_ARM, false).orElseThrow().heightPixels(), 0.0001D);
        Assertions.assertEquals(7.0D,
                profile.geometry(HumanoidSegmentId.LEFT_FOREARM, false).orElseThrow().heightPixels(), 0.0001D);
    }


    @Test
    void torsoUsesTwoSixPixelSegments() {
        SegmentedHumanoidGeometryProfile profile = profile(AnimationSkinVariant.CLASSIC);
        Assertions.assertEquals(6.0D,
                profile.geometry(HumanoidSegmentId.BODY, false).orElseThrow().heightPixels(), 0.0001D);
        Assertions.assertEquals(6.0D,
                profile.geometry(HumanoidSegmentId.BODY_LOWER, false).orElseThrow().heightPixels(), 0.0001D);
        Assertions.assertEquals(SegmentedHumanoidBone.BODY_LOWER.id(),
                SegmentedHumanoidBone.BODY.parentId().orElseThrow());
        Assertions.assertEquals(SegmentedHumanoidBone.ROOT.id(),
                SegmentedHumanoidBone.BODY_LOWER.parentId().orElseThrow());
    }

    @Test
    void legsRemainSplitIntoSixPixelSegments() {
        SegmentedHumanoidGeometryProfile profile = profile(AnimationSkinVariant.CLASSIC);
        for (HumanoidSegmentId segment : legSegments()) {
            Assertions.assertEquals(6.0D, profile.geometry(segment, false).orElseThrow().heightPixels(), 0.0001D);
        }
    }

    @Test
    void overlayGeometryFollowsEveryVisibleSegment() {
        SegmentedHumanoidGeometryProfile profile = profile(AnimationSkinVariant.CLASSIC);
        for (HumanoidSegmentId segment : HumanoidSegmentId.values()) {
            Assertions.assertTrue(profile.geometry(segment, false).isPresent());
            Assertions.assertTrue(profile.geometry(segment, true).isPresent());
        }
    }

    @Test
    void socketsAreParentedToForearms() {
        SegmentedHumanoidGeometryProfile profile = profile(AnimationSkinVariant.CLASSIC);
        var main = profile.sockets().stream()
                .filter(socket -> socket.id().equals(AnimationAttachmentSocketId.parse("main_hand")))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(SegmentedHumanoidBone.RIGHT_FOREARM.id(), main.parentBoneId());
    }

    private Set<HumanoidSegmentId> legSegments() {
        return Arrays.stream(HumanoidSegmentId.values())
                .filter(segment -> segment.name().contains("THIGH") || segment.name().contains("SHIN"))
                .collect(Collectors.toSet());
    }

    private SegmentedHumanoidGeometryProfile profile(AnimationSkinVariant variant) {
        return builder.profile(variant);
    }
}
