package io.github.kxyr0.echoanimation.forge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EchoContentPlayerFaceLayerTest {
    @Test
    void convertsContentPlayerSkinTextureBackToAuthoredSkinId() {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("echo_content_player",
                "textures/echo_player/skins/player/base.png");

        assertEquals(FaceRigId.parse("echo_content_player:player/base"),
                EchoContentPlayerFaceLayer.skinTextureRigId(texture).orElseThrow());
    }

    @Test
    void ignoresNonContentPlayerTexture() {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player.png");

        assertTrue(EchoContentPlayerFaceLayer.skinTextureRigId(texture).isEmpty());
    }

    @Test
    void addsBaseRigForAuthoredSkinOrLocalPlayer() {
        assertTrue(EchoContentPlayerFaceLayer.shouldAddBasePlayerRig(true, false));
        assertTrue(EchoContentPlayerFaceLayer.shouldAddBasePlayerRig(false, true));
        assertFalse(EchoContentPlayerFaceLayer.shouldAddBasePlayerRig(false, false));
    }

    @Test
    void prefersBasePlayerSubjectWhenBaseRigIsAvailable() {
        assertEquals(new AnimationSubjectId("echo_content_player:player/base"),
                EchoContentPlayerFaceLayer.subjectId(List.of(
                        FaceRigId.parse("echo_content_player:player/custom"),
                        FaceRigId.parse("echo_content_player:player/base"))));
    }

    @Test
    void putsBaseRigFirstForLocalPlayer() {
        assertEquals(List.of(
                        FaceRigId.parse("echo_content_player:player/base"),
                        FaceRigId.parse("echo_content_player:player/custom")),
                EchoContentPlayerFaceLayer.rigCandidates(
                        Optional.of(FaceRigId.parse("echo_content_player:player/custom")), true));
    }
}
