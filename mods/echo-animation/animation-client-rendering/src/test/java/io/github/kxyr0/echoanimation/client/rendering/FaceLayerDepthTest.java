package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.model.LayerRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FaceLayerDepthTest {
    private static final double FACE_Z = -0.251875D;

    @Test
    void tinySashaAndFatherDepthsRemainDistinctAfterFloatConversion() {
        // With the former formula the pupil and sclera collapse onto the same float plane.
        Assertions.assertEquals((float) FACE_Z, (float) (FACE_Z - .00001D * .001D));
        assertPlanes(.00001D, .0001D, .001D);
    }

    @Test
    void benDepthsAndZeroDepthsHaveTheSameStableLayerOrder() {
        assertPlanes(.01D, .02D, .03D);
        assertPlanes(0D, 0D, 0D);
    }

    @Test
    void authoredDepthStillAdjustsPositionWithinTheRoleAndLayersStayInsideHat() {
        double initial = FaceLayerDepth.resolve(FACE_Z, LayerRole.PUPIL, "pupil_left", 0D);
        double adjusted = FaceLayerDepth.resolve(FACE_Z, LayerRole.PUPIL, "pupil_left", .07D);
        Assertions.assertEquals(initial - .00007D, adjusted, 1e-12D);
        double glasses = FaceLayerDepth.resolve(FACE_Z, LayerRole.GLASSES, "glasses", .04D);
        Assertions.assertTrue(glasses > -4.5D / 16D);
        Assertions.assertEquals(initial, FaceLayerDepth.resolve(FACE_Z, LayerRole.PUPIL, "pupil_left", -1D));
    }

    private void assertPlanes(double pupil, double lid, double brow) {
        double[] depths = {
            FaceLayerDepth.resolve(FACE_Z, LayerRole.DECORATION, "sclera_left", 0D),
            FaceLayerDepth.resolve(FACE_Z, LayerRole.PUPIL, "pupil_left", pupil),
            FaceLayerDepth.resolve(FACE_Z, LayerRole.EYELID, "upper_eyelid_left", lid),
            FaceLayerDepth.resolve(FACE_Z, LayerRole.EYELID, "lower_eyelid_left", lid),
            FaceLayerDepth.resolve(FACE_Z, LayerRole.BROW, "brow_left", brow),
            FaceLayerDepth.resolve(FACE_Z, LayerRole.GLASSES, "glasses", .04D)
        };
        for (int i = 1; i < depths.length; i++) {
            Assertions.assertTrue((float) depths[i] < (float) depths[i - 1]);
            // Check transformed camera-space coordinates as well as local head coordinates.
            Assertions.assertTrue((float) (depths[i] - 8D) < (float) (depths[i - 1] - 8D));
        }
    }
}
