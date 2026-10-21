package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Authoritative NPC BODY locomotion state machine.
 *
 * <p>Navigation only supplies intent/gait. WALK/RUN can start only after real
 * horizontal displacement is observed. Passenger/sitting state is exclusive,
 * and stale navigation cannot keep locomotion alive while the body is still.</p>
 */
final class LocomotionMotionTracker {
    private static final int WINDOW_TICKS = 5;
    private static final int START_CONFIRM_WINDOWS = 2;
    private static final int STOP_CONFIRM_WINDOWS = 2;
    private static final double START_DISTANCE_BLOCKS = 0.018D;
    private static final double STOP_DISTANCE_BLOCKS = 0.006D;
    private static final double MAX_SINGLE_TICK_DISPLACEMENT = 0.75D;
    private static final double AUTHORED_WALK_SPEED_BLOCKS_PER_SECOND = 1.35D;
    private static final double AUTHORED_SPRINT_SPEED_BLOCKS_PER_SECOND = 4.30D;
    private static final double MIN_PLAYBACK_SPEED = 0.08D;
    private static final double MAX_PLAYBACK_SPEED = 1.60D;
    private static final double SPEED_ALPHA = 0.35D;
    private static final double SPEED_DEAD_BAND = 0.025D;

    private final Map<String, SubjectMotion> subjects = new HashMap<>();

    BodyState observe(
            String subject,
            double x,
            double z,
            long tick,
            NavigationGait gait,
            boolean sitting
    ) {
        SubjectMotion motion = subjects.computeIfAbsent(subject, ignored -> new SubjectMotion());
        motion.observe(x, z, tick, gait, sitting);
        return motion.state;
    }

    double speed(String subject) {
        SubjectMotion motion = subjects.get(subject);
        return motion == null ? MIN_PLAYBACK_SPEED : motion.playbackSpeed;
    }

    BodyState state(String subject) {
        SubjectMotion motion = subjects.get(subject);
        return motion == null ? BodyState.STANDING : motion.state;
    }

    void clear(String subject) {
        subjects.remove(subject);
    }

    void clearAll() {
        subjects.clear();
    }

    enum BodyState {
        STANDING,
        WALKING,
        RUNNING,
        SITTING;

        boolean locomoting() {
            return this == WALKING || this == RUNNING;
        }
    }

    private static final class SubjectMotion {
        private final ArrayDeque<MotionSample> samples = new ArrayDeque<>();
        private BodyState state = BodyState.STANDING;
        private int movingWindows;
        private int stationaryWindows;
        private double playbackSpeed = MIN_PLAYBACK_SPEED;

        void observe(double x, double z, long tick, NavigationGait gait, boolean sitting) {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                return;
            }
            if (sitting) {
                enterExclusiveState(BodyState.SITTING, x, z, tick);
                return;
            }
            if (state == BodyState.SITTING) {
                enterExclusiveState(BodyState.STANDING, x, z, tick);
                return;
            }

            MotionSample latest = samples.peekLast();
            MotionSample current = new MotionSample(x, z, tick);
            if (latest != null && tick <= latest.tick()) {
                return;
            }
            if (latest != null && horizontalDistance(latest, current) > MAX_SINGLE_TICK_DISPLACEMENT) {
                enterExclusiveState(BodyState.STANDING, x, z, tick);
                return;
            }

            samples.addLast(current);
            while (samples.size() > 2 && tick - samples.peekFirst().tick() > WINDOW_TICKS) {
                samples.removeFirst();
            }
            MotionSample first = samples.peekFirst();
            MotionSample last = samples.peekLast();
            if (first == null || last == null || last.tick() - first.tick() < WINDOW_TICKS - 1L) {
                return;
            }
            updateState(horizontalDistance(first, last), last.tick() - first.tick(), gait);
        }

        private void updateState(double distance, long elapsedTicks, NavigationGait gait) {
            boolean moving = distance >= START_DISTANCE_BLOCKS;
            boolean stationary = distance <= STOP_DISTANCE_BLOCKS;
            if (state == BodyState.STANDING) {
                stationaryWindows = 0;
                movingWindows = moving ? movingWindows + 1 : 0;
                if (movingWindows >= START_CONFIRM_WINDOWS) {
                    state = locomotionState(gait);
                    movingWindows = 0;
                    playbackSpeed = targetSpeed(distance, elapsedTicks, gait);
                }
                return;
            }
            if (!state.locomoting()) {
                return;
            }
            if (stationary) {
                stationaryWindows++;
                movingWindows = 0;
                if (stationaryWindows >= STOP_CONFIRM_WINDOWS) {
                    enterStanding();
                }
                return;
            }
            stationaryWindows = 0;
            if (moving) {
                state = locomotionState(gait);
                smoothSpeed(targetSpeed(distance, elapsedTicks, gait));
            }
        }

        private void smoothSpeed(double target) {
            if (Math.abs(target - playbackSpeed) >= SPEED_DEAD_BAND) {
                playbackSpeed += (target - playbackSpeed) * SPEED_ALPHA;
            }
        }

        private void enterStanding() {
            state = BodyState.STANDING;
            movingWindows = 0;
            stationaryWindows = 0;
            playbackSpeed = MIN_PLAYBACK_SPEED;
        }

        private void enterExclusiveState(BodyState target, double x, double z, long tick) {
            samples.clear();
            samples.addLast(new MotionSample(x, z, tick));
            state = target;
            movingWindows = 0;
            stationaryWindows = 0;
            playbackSpeed = MIN_PLAYBACK_SPEED;
        }
    }

    private static BodyState locomotionState(NavigationGait gait) {
        return gait == NavigationGait.SPRINT ? BodyState.RUNNING : BodyState.WALKING;
    }

    private static double targetSpeed(double distance, long elapsedTicks, NavigationGait gait) {
        if (elapsedTicks <= 0L) {
            return MIN_PLAYBACK_SPEED;
        }
        double blocksPerSecond = distance * 20.0D / elapsedTicks;
        double authored = gait == NavigationGait.SPRINT
                ? AUTHORED_SPRINT_SPEED_BLOCKS_PER_SECOND
                : AUTHORED_WALK_SPEED_BLOCKS_PER_SECOND;
        return clamp(blocksPerSecond / authored, MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED);
    }

    private static double horizontalDistance(MotionSample a, MotionSample b) {
        double dx = b.x() - a.x();
        double dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record MotionSample(double x, double z, long tick) {
    }
}
