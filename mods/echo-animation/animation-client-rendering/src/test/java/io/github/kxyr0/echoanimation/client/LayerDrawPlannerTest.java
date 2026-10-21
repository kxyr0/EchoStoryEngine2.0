package io.github.kxyr0.echoanimation.client;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.client.rendering.LayerDrawPlanner;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class LayerDrawPlannerTest {
    @Test
    void glassesRenderAfterEyelidsWhenOrderMatches() {
        FaceLayerDefinition eyelid = layer("upper_eyelids", LayerRole.EYELID);
        FaceLayerDefinition glasses = layer("glasses", LayerRole.GLASSES);
        var planned = new LayerDrawPlanner().plan(List.of(glasses, eyelid), Set.of());
        Assertions.assertEquals(FaceLayerId.parse("upper_eyelids"), planned.get(0).layerId());
        Assertions.assertEquals(FaceLayerId.parse("glasses"), planned.get(1).layerId());
    }

    @Test
    void hiddenLayerIsNotPlanned() {
        FaceLayerDefinition glasses = layer("glasses", LayerRole.GLASSES);
        var planned = new LayerDrawPlanner().plan(List.of(glasses), Set.of(FaceLayerId.parse("glasses")));
        Assertions.assertTrue(planned.isEmpty());
    }

    @Test
    void missingGlassesLayerDoesNotCreateGlassesCommand() {
        FaceLayerDefinition eyelid = layer("upper_eyelids", LayerRole.EYELID);
        var planned = new LayerDrawPlanner().plan(List.of(eyelid), Set.of());
        Assertions.assertTrue(planned.stream().noneMatch(command -> command.role() == LayerRole.GLASSES));
    }

    @Test
    void drawCommandKeepsLayerPlacement() {
        Transform3 placement = Transform3.IDENTITY.withTranslation(new Vec3(0.12D, 0.25D, 0.63D));
        FaceLayerDefinition brow = layer("brow_left", LayerRole.BROW, placement);
        var planned = new LayerDrawPlanner().plan(List.of(brow), Set.of());
        Assertions.assertEquals(placement, planned.get(0).placement());
    }

    private FaceLayerDefinition layer(String id, LayerRole role) {
        return layer(id, role, Transform3.IDENTITY);
    }

    private FaceLayerDefinition layer(String id, LayerRole role, Transform3 placement) {
        return new FaceLayerDefinition(
                FaceLayerId.parse(id),
                BoneId.parse("face_root"),
                FaceTextureId.parse("jdh:face/" + id),
                role,
                10,
                placement,
                new IntRect(0, 0, 8, 8),
                Optional.empty(),
                true);
    }
}
