package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class SkinnedModelReaderTest {
    private static final String IDENTITY = "[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]";

    @Test
    void adaptsLegacyRigidMeshToOneWeight() {
        String source = "{\"bones\":[{\"name\":\"root\",\"parent\":-1}],"
                + "\"meshes\":[{\"name\":\"arm\",\"bone\":0,\"vertices\":["
                + "{\"position\":[0,0,0],\"normal\":[0,1,0],\"uv\":[0,0]}],"
                + "\"triangles\":[0,0,0]}]}";

        SkinnedModel model = new SkinnedModelReader().read("legacy", new StringReader(source), 48);

        assertEquals(1, model.formatVersion());
        assertEquals(0, model.meshes().get(0).vertices().get(0).skin().index(0));
        assertEquals(1.0F, model.meshes().get(0).vertices().get(0).skin().weight(0));
    }

    @Test
    void readsVersionTwoWeightsWithoutLoss() {
        String source = "{\"formatVersion\":2,\"bones\":["
                + "{\"name\":\"root\",\"parent\":-1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "}],\"meshes\":[{\"name\":\"arm\","
                + "\"vertices\":[{\"position\":[0,0,0],\"normal\":[0,1,0],\"uv\":[0,0],"
                + "\"boneIndices\":[0],\"boneWeights\":[1]}],\"indices\":[0,0,0]}]}";

        SkinnedModel model = new SkinnedModelReader().read("v2", new StringReader(source), 48);

        assertEquals(2, model.formatVersion());
        assertEquals(1.0F, model.meshes().get(0).vertices().get(0).skin().weight(0));
    }

    @Test
    void acceptsMultipleIndependentRootsForBakedAuthoringRigs() {
        String source = "{\"formatVersion\":2,\"bones\":["
                + "{\"name\":\"root\",\"parent\":-1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "},"
                + "{\"name\":\"neutral\",\"parent\":-1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "}],\"meshes\":[{\"name\":\"body\","
                + "\"vertices\":[{\"position\":[0,0,0],\"normal\":[0,1,0],\"uv\":[0,0],"
                + "\"boneIndices\":[1],\"boneWeights\":[1]}],\"indices\":[0,0,0]}]}";

        SkinnedModel model = new SkinnedModelReader().read("multi-root", new StringReader(source), 512);

        assertEquals(2, model.bones().size());
        assertEquals(1, model.meshes().get(0).vertices().get(0).skin().index(0));
    }

    @Test
    void readsWeightedElbowFixture() {
        var stream = Objects.requireNonNull(getClass().getResourceAsStream("/skinning/elbow.skin.json"));
        var input = new InputStreamReader(stream, StandardCharsets.UTF_8);

        SkinnedModel model = new SkinnedModelReader().read("elbow", input, 48);

        assertEquals(2, model.bones().size());
        assertEquals(10, model.vertexCount());
        assertEquals(0.5F, model.meshes().get(0).vertices().get(4).skin().weight(0), 0.0001F);
        assertEquals(0.5F, model.meshes().get(0).vertices().get(4).skin().weight(1), 0.0001F);
    }
    @Test
    void readsOptionalRenderOffsetInPixels() {
        String source = "{\"formatVersion\":2,\"renderOffset\":[0,24,0],\"bones\":["
                + "{\"name\":\"root\",\"parent\":-1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "}],\"meshes\":[{\"name\":\"body\","
                + "\"vertices\":[{\"position\":[0,0,0],\"normal\":[0,1,0],\"uv\":[0,0],"
                + "\"boneIndices\":[0],\"boneWeights\":[1]}],\"indices\":[0,0,0]}]}";

        SkinnedModel model = new SkinnedModelReader().read("offset", new StringReader(source), 48);

        assertEquals(0.0F, model.renderOffsetX());
        assertEquals(24.0F, model.renderOffsetY());
        assertEquals(0.0F, model.renderOffsetZ());
    }

    @Test
    void readsAbsoluteAttachmentBindings() {
        String source = "{\"formatVersion\":2,\"attachmentBindings\":{"
                + "\"space\":\"MODEL_ABSOLUTE\",\"torsoBack\":\"body\","
                + "\"leftHand\":\"left_item\",\"rightHand\":\"right_item\","
                + "\"cancelBindBasis\":true},"
                + "\"bones\":["
                + "{\"name\":\"root\",\"parent\":-1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "},"
                + "{\"name\":\"body\",\"parent\":0,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "},"
                + "{\"name\":\"left_item\",\"parent\":1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "},"
                + "{\"name\":\"right_item\",\"parent\":1,\"bindLocal\":" + IDENTITY
                + ",\"inverseBind\":" + IDENTITY + "}],"
                + "\"meshes\":[{\"name\":\"body\",\"vertices\":["
                + "{\"position\":[0,0,0],\"normal\":[0,1,0],\"uv\":[0,0],"
                + "\"boneIndices\":[1],\"boneWeights\":[1]}],\"indices\":[0,0,0]}]}";

        SkinnedModel model = new SkinnedModelReader().read(
                "attachments",
                new StringReader(source),
                48);

        assertEquals(
                "left_item",
                model.attachmentBindings().leftHandBone());
        assertEquals(
                "MODEL_ABSOLUTE",
                model.attachmentBindings().space().name());
        assertEquals(
                true,
                model.attachmentBindings().cancelBindBasis());
    }

}
