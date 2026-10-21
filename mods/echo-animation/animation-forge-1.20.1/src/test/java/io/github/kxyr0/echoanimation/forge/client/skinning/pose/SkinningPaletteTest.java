package io.github.kxyr0.echoanimation.forge.client.skinning.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedBone;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedMesh;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class SkinningPaletteTest {
    private static final BoneId UPPER = BoneId.parse("upper_arm");
    private static final BoneId FOREARM = BoneId.parse("forearm");
    private final SkinningPalette palette = new SkinningPalette();
    private final CpuSkinningMath cpu = new CpuSkinningMath();

    @Test
    void oneWeightFollowsOneBone() {
        SkinnedModel model = twoIdentityBones(vertex(0.0F, 0.0F, SkinWeights.rigid(1, false)));
        AnimationPose pose = new AnimationPose(Map.of(FOREARM, transform(10.0D, 0.0D, 0.0D)));

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, pose, false));

        assertEquals(10.0F, position.x(), 0.0001F);
    }

    @Test
    void halfWeightsLandBetweenTransforms() {
        SkinWeights weights = SkinWeights.of(new int[]{0, 1}, new float[]{0.5F, 0.5F}, 0);
        SkinnedModel model = twoIdentityBones(vertex(0.0F, 0.0F, weights));
        AnimationPose pose = new AnimationPose(Map.of(FOREARM, transform(10.0D, 0.0D, 0.0D)));

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, pose, false));

        assertEquals(5.0F, position.x(), 0.0001F);
    }

    @Test
    void bindPoseAndInverseBindRestoreOriginalPoint() {
        Matrix4f bind = new Matrix4f().translate(2.0F, 0.0F, 0.0F);
        SkinnedModel model = new SkinnedModel(2, "bind", List.of(
                new SkinnedBone("upper_arm", -1, bind, new Matrix4f(bind).invert())),
                List.of(new SkinnedMesh("arm", List.of(vertex(3.0F, 0.0F,
                        SkinWeights.rigid(0, false))), new int[]{0, 0, 0})));

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, AnimationPose.EMPTY, true));

        assertEquals(3.0F, position.x(), 0.0001F);
    }

    @Test
    void multiplicationOrderRotatesBeforeTranslation() {
        SkinnedModel model = new SkinnedModel(2, "order", List.of(
                new SkinnedBone("upper_arm", -1, new Matrix4f(), new Matrix4f())),
                List.of(new SkinnedMesh("arm", List.of(vertex(1.0F, 0.0F,
                        SkinWeights.rigid(0, false))), new int[]{0, 0, 0})));
        AnimationPose pose = new AnimationPose(Map.of(UPPER,
                new EulerTransform(new Vec3(10.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 90.0D),
                        Vec3.ONE, Vec3.ZERO)));

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, pose, false));

        assertEquals(10.0F, position.x(), 0.0001F);
        assertEquals(1.0F, position.y(), 0.0001F);
    }

    @Test
    void ninetyDegreeElbowKeepsJointConnected() {
        Matrix4f forearmBind = new Matrix4f().translate(0.0F, 4.0F, 0.0F);
        SkinWeights weights = SkinWeights.of(new int[]{0, 1}, new float[]{0.5F, 0.5F}, 0);
        SkinnedModel model = new SkinnedModel(2, "elbow", List.of(
                new SkinnedBone("upper_arm", -1, new Matrix4f(), new Matrix4f()),
                new SkinnedBone("forearm", 0, forearmBind, new Matrix4f(forearmBind).invert())),
                List.of(new SkinnedMesh("arm", List.of(vertex(0.0F, 4.0F, weights)), new int[]{0, 0, 0})));
        EulerTransform bent = new EulerTransform(new Vec3(0.0D, 4.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 90.0D), Vec3.ONE, Vec3.ZERO);

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, new AnimationPose(Map.of(FOREARM, bent)), false));

        assertEquals(0.0F, position.x(), 0.0001F);
        assertEquals(4.0F, position.y(), 0.0001F);
    }

    @Test
    void exactMatrixPoseCanCarryShearIntoCpuSkinning() {
        SkinnedModel model = new SkinnedModel(2, "matrix", List.of(
                new SkinnedBone("upper_arm", -1, new Matrix4f(), new Matrix4f())),
                List.of(new SkinnedMesh("arm", List.of(vertex(0.0F, 4.0F,
                        SkinWeights.rigid(0, false))), new int[]{0, 0, 0})));
        Matrix4Transform shear = Matrix4Transform.fromRowMajor(
                1.0D, 0.25D, 0.0D, 2.0D,
                0.0D, 1.0D, 0.0D, 3.0D,
                0.0D, 0.0D, 1.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);

        Vector3f position = cpu.position(model.meshes().get(0).vertices().get(0),
                palette.calculate(model, AnimationPose.matrices(Map.of(UPPER, shear)), false));

        assertEquals(3.0F, position.x(), 0.0001F);
        assertEquals(7.0F, position.y(), 0.0001F);
    }

    @Test
    void bindRelativeSocketCancelsRestBasisButKeepsAnchorPosition() {
        Matrix4f bind =
                new Matrix4f()
                        .translate(6.0F, 12.0F, 0.0F)
                        .rotateX((float) Math.toRadians(90.0D))
                        .rotateY((float) Math.toRadians(180.0D));
        SkinnedModel model = new SkinnedModel(
                2,
                "socket",
                List.of(
                        new SkinnedBone(
                                "upper_arm",
                                -1,
                                bind,
                                new Matrix4f(bind).invert())),
                List.of(new SkinnedMesh(
                        "arm",
                        List.of(vertex(0.0F, 0.0F, SkinWeights.rigid(0, false))),
                        new int[]{0, 0, 0})));

        Matrix4f socket =
                palette.socketGlobal(
                        model,
                        AnimationPose.EMPTY,
                        "upper_arm",
                        true,
                        true);

        assertEquals(6.0F, socket.m30(), 0.0001F);
        assertEquals(12.0F, socket.m31(), 0.0001F);
        assertEquals(0.0F, socket.m32(), 0.0001F);
        assertEquals(1.0F, socket.m00(), 0.0001F);
        assertEquals(1.0F, socket.m11(), 0.0001F);
        assertEquals(1.0F, socket.m22(), 0.0001F);
        assertEquals(0.0F, socket.m01(), 0.0001F);
        assertEquals(0.0F, socket.m02(), 0.0001F);
        assertEquals(0.0F, socket.m10(), 0.0001F);
        assertEquals(0.0F, socket.m12(), 0.0001F);
        assertEquals(0.0F, socket.m20(), 0.0001F);
        assertEquals(0.0F, socket.m21(), 0.0001F);
    }

    private SkinnedModel twoIdentityBones(SkinnedVertex vertex) {
        return new SkinnedModel(2, "test", List.of(
                new SkinnedBone("upper_arm", -1, new Matrix4f(), new Matrix4f()),
                new SkinnedBone("forearm", 0, new Matrix4f(), new Matrix4f())),
                List.of(new SkinnedMesh("arm", List.of(vertex), new int[]{0, 0, 0})));
    }

    private SkinnedVertex vertex(float x, float y, SkinWeights weights) {
        return new SkinnedVertex(x, y, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, weights);
    }

    private EulerTransform transform(double x, double y, double rotationZ) {
        return new EulerTransform(new Vec3(x, y, 0.0D), new Vec3(0.0D, 0.0D, rotationZ),
                Vec3.ONE, Vec3.ZERO);
    }
}
