package io.github.kxyr0.echoanimation.forge.client;

import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FaceLayerQuadRendererTest {
    @Test
    void baseFaceDepthStaysNearlyFlushWithVanillaHeadSurface() {
        double faceSurfaceZ = -4.0D / 16.0D;
        double resolvedZ = FaceLayerQuadRenderer.resolvedFaceZ(Transform3.IDENTITY);

        Assertions.assertTrue(resolvedZ < faceSurfaceZ);
        Assertions.assertEquals(faceSurfaceZ - 0.03D / 16.0D, resolvedZ, 0.000001D);
    }

    @Test
    void authoredDepthOnlyBreaksDepthTiesWithoutMovingLayersAwayFromFace() {
        double baseZ = FaceLayerQuadRenderer.resolvedFaceZ(Transform3.IDENTITY);
        double raisedLayerZ = FaceLayerQuadRenderer.resolvedFaceZ(
                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.01D)));

        Assertions.assertEquals(baseZ - 0.00001D, raisedLayerZ, 0.000001D);
    }
    @Test
    void renderedSashaPupilAndUpperLowerLidsNeverShareFloatPlanes() {
        double sclera = FaceLayerQuadRenderer.resolvedFaceZ(Transform3.IDENTITY);
        double pupil = FaceLayerQuadRenderer.resolvedFaceZ(LayerRole.PUPIL, "pupil_left",
                Transform3.IDENTITY.withTranslation(new Vec3(0D, 0D, .00001D)));
        double upper = FaceLayerQuadRenderer.resolvedFaceZ(LayerRole.EYELID, "upper_eyelid_left",
                Transform3.IDENTITY.withTranslation(new Vec3(0D, 0D, .0001D)));
        double lower = FaceLayerQuadRenderer.resolvedFaceZ(LayerRole.EYELID, "lower_eyelid_left",
                Transform3.IDENTITY.withTranslation(new Vec3(0D, 0D, .0001D)));
        Assertions.assertTrue((float) pupil < (float) sclera);
        Assertions.assertTrue((float) upper < (float) pupil);
        Assertions.assertTrue((float) lower < (float) upper);
    }
}
