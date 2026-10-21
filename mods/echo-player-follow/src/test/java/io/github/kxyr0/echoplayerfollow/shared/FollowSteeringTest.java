package io.github.kxyr0.echoplayerfollow.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FollowSteeringTest {
    @Test
    void idleInsideStopDistance() {
        FollowSteering.InputPlan plan = FollowSteering.plan(0.0D, 4.0D, 0.0D, 1.0D);

        assertFalse(plan.moving());
        assertFalse(plan.sprint());
    }

    @Test
    void walksForwardWhenGoalIsInFrontOfCamera() {
        FollowSteering.InputPlan plan = FollowSteering.plan(0.0D, 4.0D, 0.0D, 2.5D);

        assertEquals(0.88D, plan.forwardImpulse(), 0.001D);
        assertEquals(0.0D, plan.leftImpulse(), 0.001D);
        assertFalse(plan.sprint());
    }

    @Test
    void strafesLeftTowardWorldEastWithoutChangingCamera() {
        FollowSteering.InputPlan plan = FollowSteering.plan(4.0D, 0.0D, 0.0D, 2.5D);

        assertEquals(0.0D, plan.forwardImpulse(), 0.001D);
        assertEquals(0.88D, plan.leftImpulse(), 0.001D);
    }

    @Test
    void sprintsWhenFarRegardlessOfCameraDirection() {
        FollowSteering.InputPlan forward = FollowSteering.plan(0.0D, 8.0D, 0.0D, 6.5D);
        FollowSteering.InputPlan side = FollowSteering.plan(8.0D, 0.0D, 0.0D, 6.5D);

        assertTrue(forward.sprint());
        assertTrue(side.sprint());
    }

    @Test
    void keepsWorldDirectionStableWhenCameraTurns() {
        FollowSteering.InputPlan plan = FollowSteering.plan(0.0D, 4.0D, 90.0D, 2.5D);
        double[] world = worldVector(plan, 90.0D);

        assertEquals(0.0D, world[0], 0.001D);
        assertEquals(0.88D, world[1], 0.001D);
    }

    private double[] worldVector(FollowSteering.InputPlan plan, double yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double x = plan.leftImpulse() * Math.cos(yaw) - plan.forwardImpulse() * Math.sin(yaw);
        double z = plan.leftImpulse() * Math.sin(yaw) + plan.forwardImpulse() * Math.cos(yaw);
        return new double[] {x, z};
    }
}
