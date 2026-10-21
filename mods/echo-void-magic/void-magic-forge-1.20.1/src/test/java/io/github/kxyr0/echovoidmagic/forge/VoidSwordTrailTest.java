package io.github.kxyr0.echovoidmagic.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VoidSwordTrailTest {
    @Test
    void bladeCrossesFromUpperRightToLowerLeft() {
        Vec3 eye = new Vec3(0.0D, 1.62D, 0.0D);
        Vec3 forward = new Vec3(0.0D, 0.0D, -1.0D);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        List<Vec3> start = VoidSwordTrail.bladePoints(eye, forward, up, 0.0D);
        List<Vec3> end = VoidSwordTrail.bladePoints(eye, forward, up, 1.0D);

        assertEquals(6, start.size());
        assertEquals(6, end.size());
        assertTrue(start.get(5).x > 0.0D);
        assertTrue(start.get(5).y > end.get(5).y);
        assertTrue(end.get(5).x < 0.0D);
        assertTrue(start.stream().allMatch(VoidSwordTrailTest::finite));
        assertTrue(end.stream().allMatch(VoidSwordTrailTest::finite));
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
