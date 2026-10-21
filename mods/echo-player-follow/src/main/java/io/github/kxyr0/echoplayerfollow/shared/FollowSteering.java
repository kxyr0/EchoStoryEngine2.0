package io.github.kxyr0.echoplayerfollow.shared;

public final class FollowSteering {
    private FollowSteering() {
    }

    public static InputPlan plan(double targetDx, double targetDz, double yawDegrees, double npcDistance) {
        double length = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        if (npcDistance < FollowConstants.STOP_DISTANCE || length < FollowConstants.SPEED_IDLE_DISTANCE) {
            return InputPlan.idle();
        }
        double x = targetDx / length;
        double z = targetDz / length;
        double yaw = Math.toRadians(yawDegrees);
        double speed = speedFor(length);
        double forward = dot(x, z, -Math.sin(yaw), Math.cos(yaw));
        double left = dot(x, z, Math.cos(yaw), Math.sin(yaw));
        boolean sprint = npcDistance > FollowConstants.SPRINT_START_DISTANCE && length > FollowConstants.WALK_DISTANCE;
        return new InputPlan(clamp(forward * speed), clamp(left * speed), sprint);
    }

    private static double dot(double ax, double az, double bx, double bz) {
        return ax * bx + az * bz;
    }

    private static double clamp(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }

    private static double speedFor(double goalDistance) {
        double normalized = (goalDistance - FollowConstants.SPEED_IDLE_DISTANCE) / 3.0D;
        double curved = Math.sqrt(Math.max(0.0D, Math.min(1.0D, normalized)));
        return 0.32D + curved * 0.56D;
    }

    public record InputPlan(double forwardImpulse, double leftImpulse, boolean sprint) {
        public static InputPlan idle() {
            return new InputPlan(0.0D, 0.0D, false);
        }

        public boolean moving() {
            return Math.abs(forwardImpulse) > 0.01D || Math.abs(leftImpulse) > 0.01D;
        }
    }
}
