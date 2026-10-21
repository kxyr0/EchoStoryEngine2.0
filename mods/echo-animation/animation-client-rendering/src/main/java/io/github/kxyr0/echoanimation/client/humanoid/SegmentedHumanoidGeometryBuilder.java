package io.github.kxyr0.echoanimation.client.humanoid;

import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationAttachmentSocketDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds A2 segmented humanoid descriptors over a standard 64x64 skin atlas.
 */
public final class SegmentedHumanoidGeometryBuilder {
    public SegmentedHumanoidGeometryProfile profile(AnimationSkinVariant variant) {
        double armWidth = variant == AnimationSkinVariant.SLIM ? 3.0D : 4.0D;
        List<HumanoidSegmentGeometry> geometry = new ArrayList<>();
        add(geometry, HumanoidSegmentId.HEAD, new Dimensions(8.0D, 8.0D, 8.0D), uvs(0, 0, 32, 0));
        add(geometry, HumanoidSegmentId.BODY, new Dimensions(8.0D, 6.0D, 4.0D),
                uvs(16, 16, 16, 32).withSize(8, 6));
        add(geometry, HumanoidSegmentId.BODY_LOWER, new Dimensions(8.0D, 6.0D, 4.0D),
                uvs(16, 22, 16, 38).withSize(8, 6));
        addArm(geometry, HumanoidSegmentId.RIGHT_UPPER_ARM, armWidth, 5.0D, uvs(40, 16, 40, 32));
        addArm(geometry, HumanoidSegmentId.RIGHT_FOREARM, armWidth, 7.0D, uvs(40, 16, 40, 32));
        addArm(geometry, HumanoidSegmentId.LEFT_UPPER_ARM, armWidth, 5.0D, uvs(32, 48, 48, 48));
        addArm(geometry, HumanoidSegmentId.LEFT_FOREARM, armWidth, 7.0D, uvs(32, 48, 48, 48));
        addLeg(geometry, HumanoidSegmentId.RIGHT_THIGH, uvs(0, 16, 0, 32));
        addLeg(geometry, HumanoidSegmentId.RIGHT_SHIN, uvs(0, 16, 0, 32));
        addLeg(geometry, HumanoidSegmentId.LEFT_THIGH, uvs(16, 48, 0, 48));
        addLeg(geometry, HumanoidSegmentId.LEFT_SHIN, uvs(16, 48, 0, 48));
        return new SegmentedHumanoidGeometryProfile(variant, geometry, sockets());
    }

    private void addArm(
            List<HumanoidSegmentGeometry> result,
            HumanoidSegmentId segment,
            double width,
            double height,
            UvPair uvPair
    ) {
        add(result, segment, new Dimensions(width, height, 4.0D), uvPair.withSize((int) width, (int) height));
    }

    private void addLeg(
            List<HumanoidSegmentGeometry> result,
            HumanoidSegmentId segment,
            UvPair uvPair
    ) {
        add(result, segment, new Dimensions(4.0D, 6.0D, 4.0D), uvPair.withSize(4, 6));
    }

    private void add(
            List<HumanoidSegmentGeometry> result,
            HumanoidSegmentId segment,
            Dimensions dimensions,
            UvPair uvPair
    ) {
        result.add(new HumanoidSegmentGeometry(segment, dimensions.width(), dimensions.height(),
                dimensions.depth(), uvPair.base(), false));
        result.add(new HumanoidSegmentGeometry(segment, dimensions.width(), dimensions.height(),
                dimensions.depth(), uvPair.overlay(), true));
    }

    private List<AnimationAttachmentSocketDefinition> sockets() {
        return List.of(
                socket("main_hand", SegmentedHumanoidBone.RIGHT_FOREARM, 0.0D),
                socket("off_hand", SegmentedHumanoidBone.LEFT_FOREARM, 0.0D));
    }

    private AnimationAttachmentSocketDefinition socket(String id, SegmentedHumanoidBone parent, double z) {
        EulerTransform local = EulerTransform.IDENTITY.withTranslation(new Vec3(0.0D, -6.0D, z));
        return new AnimationAttachmentSocketDefinition(AnimationAttachmentSocketId.parse(id), parent.id(), local);
    }

    private UvPair uvs(int baseX, int baseY, int overlayX, int overlayY) {
        return new UvPair(baseX, baseY, overlayX, overlayY, 8, 8);
    }

    private record Dimensions(double width, double height, double depth) {
    }

    private record UvPair(int baseX, int baseY, int overlayX, int overlayY, int width, int height) {
        private UvPair withSize(int nextWidth, int nextHeight) {
            return new UvPair(baseX, baseY, overlayX, overlayY, nextWidth, nextHeight);
        }

        private IntRect base() {
            return new IntRect(baseX, baseY, width, height);
        }

        private IntRect overlay() {
            return new IntRect(overlayX, overlayY, width, height);
        }
    }
}
