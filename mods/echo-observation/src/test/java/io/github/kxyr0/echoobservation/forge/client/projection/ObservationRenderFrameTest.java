package io.github.kxyr0.echoobservation.forge.client.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class ObservationRenderFrameTest {
    @Test
    void completeViewProjectionKeepsWorldPointAtRenderedPosition() {
        Matrix4f view = new Matrix4f().translate(-1.0F, 0.0F, 0.0F);
        Matrix4f projection = new Matrix4f().setPerspective(
                (float) Math.toRadians(70.0D), 16.0F / 9.0F, 0.05F, 1000.0F);
        ObservationRenderFrame frame = ObservationRenderFrame.create(
                view, projection, Vec3.ZERO, 1920, 1080);
        ObservationAnchor anchor = new ObservationAnchor("minecraft:overworld", 1, 0, -5,
                0.0D, 0.0D, 0.0D, ObservationFace.SOUTH);

        ObservationScreenPoint point = frame.project(anchor).orElseThrow();

        assertEquals(960.0D, point.framebufferX(), 0.001D);
        assertEquals(540.0D, point.framebufferY(), 0.001D);
    }
}
