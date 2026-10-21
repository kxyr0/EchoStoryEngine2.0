package io.github.kxyr0.echoobservation.authoring.forge.client.render;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationAuthoringWorldRendererTest {
    @Test
    void storedWorldPointDoesNotMoveWithCamera() {
        ObservationAnchor anchor = new ObservationAnchor(
                "minecraft:overworld", 2, -2, 415, 0.0D, 0.133D, 0.585D, ObservationFace.WEST);
        assertWorldPoint(anchor, new Vec3(4.5D, 0.25D, 410.0D));
        assertWorldPoint(anchor, new Vec3(-30.0D, 18.0D, 450.0D));
    }

    @Test
    void visibleMarkerStaysWorldAnchoredJustOutsideTheSurface() {
        ObservationAnchor anchor = new ObservationAnchor(
                "minecraft:overworld", 2, -2, 415, 0.0D, 0.605D, 0.060D, ObservationFace.WEST);
        assertVisiblePoint(anchor, new Vec3(4.5D, 0.25D, 410.0D));
        assertVisiblePoint(anchor, new Vec3(-30.0D, 18.0D, 450.0D));
    }

    private void assertWorldPoint(ObservationAnchor anchor, Vec3 camera) {
        Vec3 relative = ObservationAuthoringWorldRenderer.renderPoint(anchor, camera);
        assertEquals(anchor.worldX(), relative.x + camera.x, 0.000_001D);
        assertEquals(anchor.worldY(), relative.y + camera.y, 0.000_001D);
        assertEquals(anchor.worldZ(), relative.z + camera.z, 0.000_001D);
    }

    private void assertVisiblePoint(ObservationAnchor anchor, Vec3 camera) {
        Vec3 relative = ObservationAuthoringWorldRenderer.visiblePoint(anchor, camera);
        assertEquals(anchor.worldX() - 0.015D, relative.x + camera.x, 0.000_001D);
        assertEquals(anchor.worldY(), relative.y + camera.y, 0.000_001D);
        assertEquals(anchor.worldZ(), relative.z + camera.z, 0.000_001D);
    }
}
