package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.HashMap;
import java.util.Map;

final class LocomotionMotionTracker {
    private static final double DEFAULT_PLAYBACK_SPEED = 1.0D;
    private static final double AUTHORED_WALK_SPEED_BLOCKS_PER_SECOND = 1.35D;
    private static final double AUTHORED_SPRINT_SPEED_BLOCKS_PER_SECOND = 4.3D;
    private static final double MIN_PLAYBACK_SPEED = 0.35D;
    private static final double MAX_PLAYBACK_SPEED = 3.0D;
    private static final double MAX_TRACKED_BLOCKS_PER_SECOND = 20.0D;
    private static final double SPEED_SMOOTHING_ALPHA = 0.25D;
    private static final double MAX_PLAYBACK_SPEED_STEP = 0.12D;
    private static final double MOVEMENT_EPSILON_BLOCKS = 0.0025D;
    private static final long STATIONARY_GRACE_TICKS = 12L;

    private final Map<String, MotionSample> samples = new HashMap<>();
    private final Map<String, Double> playbackSpeeds = new HashMap<>();
    private final Map<String, Long> lastPhysicalMotionTick = new HashMap<>();

    double update(String subject, NavigationProgressSnapshot progress, NavigationGait gait,
            boolean synchronize, long adapterTick) {
        MotionSample current = sample(progress);
        MotionSample previous = samples.put(subject, current);
        lastPhysicalMotionTick.putIfAbsent(subject, adapterTick);
        if (previous != null && horizontalDistance(previous, current) > MOVEMENT_EPSILON_BLOCKS) {
            lastPhysicalMotionTick.put(subject, adapterTick);
        }
        if (!synchronize) {
            playbackSpeeds.put(subject, DEFAULT_PLAYBACK_SPEED);
            return DEFAULT_PLAYBACK_SPEED;
        }
        if (previous == null || current.serverTick() <= previous.serverTick()) {
            return speed(subject);
        }
        double blocksPerSecond = blocksPerSecond(previous, current);
        if (!Double.isFinite(blocksPerSecond) || blocksPerSecond > MAX_TRACKED_BLOCKS_PER_SECOND) {
            return speed(subject);
        }
        double targetSpeed = clamp(blocksPerSecond / authoredSpeed(gait), MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED);
        double previousSpeed = speed(subject);
        double speed = previousSpeed + clamp((targetSpeed - previousSpeed) * SPEED_SMOOTHING_ALPHA,
                -MAX_PLAYBACK_SPEED_STEP, MAX_PLAYBACK_SPEED_STEP);
        playbackSpeeds.put(subject, speed);
        return speed;
    }

    boolean shouldAnimate(String subject, long adapterTick) {
        Long lastMotion = lastPhysicalMotionTick.get(subject);
        return lastMotion == null || adapterTick - lastMotion <= STATIONARY_GRACE_TICKS;
    }

    double speed(String subject) {
        return playbackSpeeds.getOrDefault(subject, DEFAULT_PLAYBACK_SPEED);
    }

    void clear(String subject) {
        samples.remove(subject);
        playbackSpeeds.remove(subject);
        lastPhysicalMotionTick.remove(subject);
    }

    private static double authoredSpeed(NavigationGait gait) {
        return gait == NavigationGait.SPRINT
                ? AUTHORED_SPRINT_SPEED_BLOCKS_PER_SECOND : AUTHORED_WALK_SPEED_BLOCKS_PER_SECOND;
    }

    private static MotionSample sample(NavigationProgressSnapshot progress) {
        Vector3 position = progress.currentPosition();
        return new MotionSample(position.x(), position.y(), position.z(), progress.serverTick());
    }

    private static double horizontalDistance(MotionSample previous, MotionSample current) {
        double dx = current.x() - previous.x();
        double dz = current.z() - previous.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double blocksPerSecond(MotionSample previous, MotionSample current) {
        double distance = horizontalDistance(previous, current);
        double seconds = (current.serverTick() - previous.serverTick()) / 20.0D;
        return seconds <= 0.0D ? 0.0D : distance / seconds;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record MotionSample(double x, double y, double z, long serverTick) {
    }
}
