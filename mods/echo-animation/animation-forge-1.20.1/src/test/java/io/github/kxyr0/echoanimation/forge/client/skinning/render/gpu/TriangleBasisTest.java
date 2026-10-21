package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class TriangleBasisTest {
    @Test
    void calculatesQuadCompatibleMidpointAndTangent() {
        TriangleBasis basis = TriangleBasis.calculate(
                vertex(0, 0, 0, 0), vertex(1, 0, 1, 0), vertex(0, 1, 0, 1));

        Assertions.assertEquals(0.5F, basis.midU(), 0.0001F);
        Assertions.assertEquals(0.5F, basis.midV(), 0.0001F);
        Assertions.assertEquals(1.0F, basis.x(), 0.0001F);
        Assertions.assertEquals(0.0F, basis.y(), 0.0001F);
        Assertions.assertEquals(0.0F, basis.z(), 0.0001F);
        Assertions.assertEquals(1.0F, basis.handedness(), 0.0001F);
    }

    private SkinnedVertex vertex(float x, float y, float u, float v) {
        return new SkinnedVertex(x, y, 0, 0, 0, 1, u, v, SkinWeights.rigid(0, false));
    }
}
