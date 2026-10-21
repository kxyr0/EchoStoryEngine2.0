package io.github.kxyr0.echoik.forge.command;

import io.github.kxyr0.echoik.core.math.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class NpcTargetSpaceTest {
    private final NpcTargetSpace space = new NpcTargetSpace();

    @Test
    void convertsMinecraftForwardDirectionToEchoForwardForCardinalYaw() {
        assertNear(new Vec3(0.0D, 0.0D, 16.0D), local(0.0F, 0.0D, 0.0D, 1.0D));
        assertNear(new Vec3(0.0D, 0.0D, 16.0D), local(180.0F, 0.0D, 0.0D, -1.0D));
        assertNear(new Vec3(0.0D, 0.0D, 16.0D), local(-90.0F, 1.0D, 0.0D, 0.0D));
        assertNear(new Vec3(0.0D, 0.0D, 16.0D), local(90.0F, -1.0D, 0.0D, 0.0D));
    }

    @Test
    void convertsMinecraftBackwardDirectionToEchoNegativeZForCardinalYaw() {
        assertNear(new Vec3(0.0D, 0.0D, -16.0D), local(0.0F, 0.0D, 0.0D, -1.0D));
        assertNear(new Vec3(0.0D, 0.0D, -16.0D), local(180.0F, 0.0D, 0.0D, 1.0D));
        assertNear(new Vec3(0.0D, 0.0D, -16.0D), local(-90.0F, -1.0D, 0.0D, 0.0D));
        assertNear(new Vec3(0.0D, 0.0D, -16.0D), local(90.0F, 1.0D, 0.0D, 0.0D));
    }

    @Test
    void convertsMinecraftLeftDirectionToEchoPositiveXForCardinalYaw() {
        assertNear(new Vec3(16.0D, 0.0D, 0.0D), local(0.0F, 1.0D, 0.0D, 0.0D));
        assertNear(new Vec3(16.0D, 0.0D, 0.0D), local(180.0F, -1.0D, 0.0D, 0.0D));
        assertNear(new Vec3(16.0D, 0.0D, 0.0D), local(-90.0F, 0.0D, 0.0D, -1.0D));
        assertNear(new Vec3(16.0D, 0.0D, 0.0D), local(90.0F, 0.0D, 0.0D, 1.0D));
    }

    @Test
    void keepsWorldYAsEchoPixelOffsetFromNpcFeet() {
        assertNear(new Vec3(0.0D, 24.0D, 0.0D),
                space.toLocal(10.0D, -1.0D, 20.0D, 0.0F, 10.0D, 0.5D, 20.0D));
    }

    @Test
    void liftsStandingTargetFromFeetToTorsoHeight() {
        assertNear(new Vec3(0.0D, 16.8D, 0.0D),
                space.toLocalStandingTarget(10.0D, -1.0D, 20.0D, 0.0F, 10.0D, -1.0D, 20.0D));
    }

    private Vec3 local(float bodyYaw, double worldX, double worldY, double worldZ) {
        return space.toLocal(0.0D, 0.0D, 0.0D, bodyYaw, worldX, worldY, worldZ);
    }

    private void assertNear(Vec3 expected, Vec3 actual) {
        Assertions.assertEquals(expected.x(), actual.x(), 0.0001D);
        Assertions.assertEquals(expected.y(), actual.y(), 0.0001D);
        Assertions.assertEquals(expected.z(), actual.z(), 0.0001D);
    }
}
