package io.github.kxyr0.echocutscene.forge.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echocutscene.api.camera.CameraFieldOfView;
import io.github.kxyr0.echocutscene.api.camera.CameraKeyframe;
import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.api.camera.CameraPosition;
import io.github.kxyr0.echocutscene.api.camera.CameraRotation;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackMetadata;
import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackDataId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class ClientboundCutsceneStartPacketTest {
    @Test
    void packetCarriesCinematicHudPolicy() {
        CutsceneHudPolicy policy = new CutsceneHudPolicy(true, true, false, false);
        ClientboundCutsceneStartPacket packet = new ClientboundCutsceneStartPacket(
                new CutscenePlaybackId("playback-1"), track(), 120L, 1.0D,
                CutsceneEndBehavior.HOLD_LAST_FRAME, policy);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        packet.encode(buffer);
        ClientboundCutsceneStartPacket decoded = ClientboundCutsceneStartPacket.decode(buffer);

        assertFalse(decoded.hudPolicy().cinematic());
        assertTrue(decoded.hudPolicy().hideGameplayHud());
        assertTrue(decoded.hudPolicy().keepDialogueVisible());
    }

    private static CameraTrackSnapshot track() {
        CameraTrackMetadata metadata = new CameraTrackMetadata(CameraTrackId.parse("jdh:test_camera"),
                new CameraTrackRevision(1L), new CameraTrackDataId("test.camera.es"), 1000L, 60, 1, "test");
        return new CameraTrackSnapshot(metadata, List.of(frame(0L), frame(1000L)));
    }

    private static CameraKeyframe frame(long timeMillis) {
        CameraPose pose = new CameraPose(new CameraPosition(new Vector3(0.0D, 64.0D, 0.0D)),
                new CameraRotation(0.0D, 0.0D, 0.0D), new CameraFieldOfView(70.0D));
        return new CameraKeyframe(timeMillis, pose, CameraInterpolation.LINEAR);
    }
}
