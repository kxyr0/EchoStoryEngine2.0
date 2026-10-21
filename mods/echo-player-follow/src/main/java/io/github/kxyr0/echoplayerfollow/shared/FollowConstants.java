package io.github.kxyr0.echoplayerfollow.shared;

public final class FollowConstants {
    public static final int SESSION_VALIDATE_INTERVAL_TICKS = 10;
    public static final int AUTO_PROMPT_SCAN_INTERVAL_TICKS = 40;
    public static final int NAVIGATION_SCAN_LIMIT = 128;
    public static final long HOLD_MILLIS = 1000L;
    public static final long PROBE_INTERVAL_MILLIS = 350L;
    public static final long MANUAL_CANCEL_GRACE_MILLIS = 1200L;
    public static final long PROMPT_TTL_MILLIS = 2500L;
    public static final double PROMPT_RADIUS = 7.0D;
    public static final double STOP_DISTANCE = 1.15D;
    public static final double WALK_DISTANCE = 3.0D;
    public static final double CANCEL_DISTANCE = 18.0D;
    public static final double FOLLOW_DISTANCE = 1.35D;
    public static final double GOAL_DEADBAND = 0.30D;
    public static final double SPEED_IDLE_DISTANCE = 0.60D;
    public static final double SPRINT_START_DISTANCE = 5.2D;
    public static final double SPRINT_STOP_DISTANCE = 3.7D;
    public static final double NPC_STOP_SPEED = 0.015D;

    private FollowConstants() {
    }
}
