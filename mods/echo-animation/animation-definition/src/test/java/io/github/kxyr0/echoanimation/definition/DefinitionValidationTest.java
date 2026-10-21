package io.github.kxyr0.echoanimation.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DefinitionValidationTest {
    @Test
    void unknownLayerBoneIsRejected() {
        FaceLayerDefinition layer = new FaceLayerDefinition(
                FaceLayerId.parse("glasses"),
                BoneId.parse("missing"),
                FaceTextureId.parse("jdh:face/glasses"),
                LayerRole.GLASSES,
                50,
                Transform3.IDENTITY,
                new IntRect(0, 0, 8, 8),
                Optional.empty(),
                true);
        FaceRigDefinition rig = new FaceRigDefinition(
                FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(BoneDefinition.root(BoneId.parse("face_root")))),
                List.of(layer),
                List.of(),
                "fingerprint");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FaceRigValidator().validate(rig));
    }
}
