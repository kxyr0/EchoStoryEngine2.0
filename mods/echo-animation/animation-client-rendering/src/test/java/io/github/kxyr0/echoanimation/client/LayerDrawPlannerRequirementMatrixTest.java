package io.github.kxyr0.echoanimation.client;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.client.rendering.LayerDrawPlanner;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class LayerDrawPlannerRequirementMatrixTest {
    @Test
    void drawOrderIsDeterministicByOrderRoleAndId() {
        var planned = new LayerDrawPlanner().plan(List.of(layer("z_layer", LayerRole.EYELID, 20, Optional.empty()),
                layer("a_layer", LayerRole.EYELID, 20, Optional.empty()),
                layer("skin", LayerRole.SKIN, 10, Optional.empty())), Set.of());
        Assertions.assertEquals(FaceLayerId.parse("skin"), planned.get(0).layerId());
        Assertions.assertEquals(FaceLayerId.parse("a_layer"), planned.get(1).layerId());
        Assertions.assertEquals(FaceLayerId.parse("z_layer"), planned.get(2).layerId());
    }

    @Test
    void hiddenGlassesForInspectionAreNotPlanned() {
        var planned = new LayerDrawPlanner().plan(List.of(layer("glasses", LayerRole.GLASSES, 30, Optional.empty())),
                Set.of(FaceLayerId.parse("glasses")));
        Assertions.assertTrue(planned.isEmpty());
    }

    @Test
    void drawCommandKeepsClipRect() {
        IntRect clip = new IntRect(16, 16, 8, 8);
        var planned = new LayerDrawPlanner().plan(List.of(layer("pupil_left", LayerRole.PUPIL, 10, Optional.of(clip))),
                Set.of());
        Assertions.assertEquals(Optional.of(clip), planned.get(0).clipRect());
    }

    @Test
    void pupilClipBoundsAreRepresentedByLayerClip() {
        IntRect clip = new IntRect(16, 16, 8, 8);
        Assertions.assertTrue(layer("pupil_left", LayerRole.PUPIL, 10, Optional.of(clip))
                .clipRect()
                .orElseThrow()
                .fitsInside(64, 64));
    }

    private FaceLayerDefinition layer(String id, LayerRole role, int order, Optional<IntRect> clip) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), BoneId.parse("face_root"),
                FaceTextureId.parse("jdh:face/" + id), role, order, Transform3.IDENTITY,
                new IntRect(0, 0, 8, 8), clip, true);
    }
}
