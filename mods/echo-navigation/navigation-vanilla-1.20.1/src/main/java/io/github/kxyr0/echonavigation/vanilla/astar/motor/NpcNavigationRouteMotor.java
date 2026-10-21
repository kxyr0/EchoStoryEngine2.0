package io.github.kxyr0.echonavigation.vanilla.astar.motor;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.api.route.NavigationRoute;
import io.github.kxyr0.echonavigation.api.route.NavigationRouteNode;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalEdge;
import io.github.kxyr0.echonavigation.api.route.NavigationTraversalType;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import io.github.kxyr0.echonavigation.vanilla.astar.surface.NavigationAgentProfile;
import io.github.kxyr0.echonavigation.vanilla.path.VanillaDoorSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class NpcNavigationRouteMotor {
    private static final double NODE_TOLERANCE = 0.42D;
    private static final double TERRAIN_TOLERANCE = 0.66D;
    private static final double LADDER_TOLERANCE = 0.34D;
    private static final double JUMP_SPEED = 0.24D;
    private static final int BLOCKED_TICKS_BEFORE_REPLAN = 20;
    private final NavigationAgentProfile profile;
    private final double movementSpeedPerTick;
    private final AStarLadderMotor ladders = new AStarLadderMotor();
    private NavigationRoute route;
    private int edgeIndex;
    private int blockedTicks;

    public NpcNavigationRouteMotor(NavigationAgentProfile profile) {
        this(profile, profile.movementSpeed() * 20.0D);
    }

    public NpcNavigationRouteMotor(NavigationAgentProfile profile, double speedBlocksPerSecond) {
        this.profile = profile;
        movementSpeedPerTick = Math.max(0.0025D, Math.min(1.0D, speedBlocksPerSecond / 20.0D));
    }

    public void start(NavigationRoute route) {
        this.route = route;
        edgeIndex = 0;
        blockedTicks = 0;
    }

    public Tick tick(NavigationAgentHandle handle, VanillaDoorSupport doors) {
        if (route == null || edgeIndex >= route.edges().size()) {
            ladders.release(handle.mob());
            return new Tick(Status.ARRIVED, handle.position(), 0.0D, edgeIndex, "Route complete.");
        }
        NavigationTraversalEdge edge = route.edges().get(edgeIndex);
        NavigationRouteNode from = route.nodes().get(edge.fromNodeIndex());
        NavigationRouteNode to = route.nodes().get(edge.toNodeIndex());
        if (door(edge.type())) {
            doors.openAround(handle, new BlockPos(to.blockX(), to.blockY(), to.blockZ()));
        }
        if (reached(handle.mob(), to, edge.type())) {
            edgeIndex++;
            blockedTicks = 0;
            return tick(handle, doors);
        }
        boolean climb = edge.type() != NavigationTraversalType.JUMP
                && (ladder(edge.type()) || ladders.controls(handle, from, to));
        Vector3 before = handle.position();
        Vec3 requested = climb ? ladders.move(handle, from, to) : requestedDelta(handle.mob(), from, to, edge.type());
        if (edge.type() == NavigationTraversalType.JUMP) {
            ladders.release(handle.mob());
            moveJump(handle.mob(), requested, to);
        } else if (!climb) {
            ladders.release(handle.mob());
            move(handle.mob(), requested, to);
        }
        Vector3 after = handle.position();
        if (climb) {
            blockedTicks = 0;
        } else {
            trackBlocked(requested, before, after);
        }
        Status status = blockedTicks >= BLOCKED_TICKS_BEFORE_REPLAN ? Status.REPLAN_REQUESTED : Status.MOVING;
        return new Tick(status, after, remainingDistance(handle.mob(), edge), edgeIndex, "A* route progressing.");
    }

    public void stop(NavigationAgentHandle handle) {
        route = null;
        ladders.release(handle.mob());
        handle.mob().setDeltaMovement(Vec3.ZERO);
    }

    public NavigationRoute route() { return route; }

    public int edgeIndex() { return edgeIndex; }

    private Vec3 requestedDelta(Mob mob, NavigationRouteNode from, NavigationRouteNode to,
            NavigationTraversalType type) {
        double targetX = to.blockX() + 0.5D;
        double targetZ = to.blockZ() + 0.5D;
        double dx = targetX - mob.getX();
        double dz = targetZ - mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double requestedSpeed = type == NavigationTraversalType.JUMP
                ? Math.min(JUMP_SPEED, movementSpeedPerTick) : movementSpeedPerTick;
        double speed = Math.min(requestedSpeed,
                Math.max(0.05D, horizontal));
        double mx = horizontal < 0.001D ? 0.0D : dx / horizontal * speed;
        double mz = horizontal < 0.001D ? 0.0D : dz / horizontal * speed;
        double my = verticalDelta(mob, from, to, type);
        return new Vec3(mx, my, mz);
    }

    private double verticalDelta(Mob mob, NavigationRouteNode from, NavigationRouteNode to,
            NavigationTraversalType type) {
        double dy = to.feetY() - mob.getY();
        if (type == NavigationTraversalType.LADDER_UP || type == NavigationTraversalType.LADDER_DOWN) {
            return clamp(dy, -0.14D, 0.14D);
        }
        if (to.height16() < from.height16()) {
            return clamp(dy, -0.18D, 0.0D);
        }
        if (to.height16() > from.height16()) {
            return clamp(dy, 0.0D, 0.12D);
        }
        return clamp(dy, -0.05D, 0.05D);
    }

    private void move(Mob mob, Vec3 requested, NavigationRouteNode target) {
        if (requested.horizontalDistance() > 0.001D) {
            faceMovement(mob, requested);
        }
        mob.getNavigation().stop();
        mob.setMaxUpStep((float) Math.max(mob.maxUpStep(), profile.maximumStepUp()));
        mob.move(MoverType.SELF, requested);
        mob.getMoveControl().setWantedPosition(target.blockX() + 0.5D, target.feetY(),
                target.blockZ() + 0.5D, 0.0D);
    }

    private void faceMovement(Mob mob, Vec3 requested) {
        float yaw = (float) (Math.atan2(requested.z, requested.x) * 180.0D / Math.PI) - 90.0F;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.yRotO = yaw;
        mob.yHeadRotO = yaw;
        mob.yBodyRotO = yaw;
    }

    private void moveJump(Mob mob, Vec3 requested, NavigationRouteNode target) {
        move(mob, requested, target);
        double horizontal = requested.horizontalDistance();
        double lift = horizontal > 0.08D && mob.getY() < target.feetY() + 0.35D ? 0.08D : 0.0D;
        mob.setDeltaMovement(requested.x, Math.max(requested.y, lift), requested.z);
        mob.resetFallDistance();
        mob.hasImpulse = true;
    }

    private boolean reached(Mob mob, NavigationRouteNode node, NavigationTraversalType type) {
        double dx = mob.getX() - (node.blockX() + 0.5D);
        double dz = mob.getZ() - (node.blockZ() + 0.5D);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double vertical = Math.abs(mob.getY() - node.feetY());
        double tolerance = horizontalTolerance(type);
        return horizontal <= tolerance && vertical <= (ladder(type) ? 0.25D : 0.5D);
    }

    private void trackBlocked(Vec3 requested, Vector3 before, Vector3 after) {
        double requestedHorizontal = Math.sqrt(requested.x * requested.x + requested.z * requested.z);
        double actualHorizontal = before.horizontalDistanceTo(after);
        double actualVertical = Math.abs(before.y() - after.y());
        if (requestedHorizontal > 0.04D && actualHorizontal < requestedHorizontal * 0.2D
                && actualVertical < 0.015D) {
            blockedTicks++;
        } else {
            blockedTicks = 0;
        }
    }

    private double remainingDistance(Mob mob, NavigationTraversalEdge currentEdge) {
        double remaining = edgeRemainder(mob, currentEdge);
        for (int index = edgeIndex + 1; index < route.edges().size(); index++) {
            remaining += route.edges().get(index).cost();
        }
        return Math.max(0.0D, remaining);
    }

    private double edgeRemainder(Mob mob, NavigationTraversalEdge edge) {
        NavigationRouteNode from = route.nodes().get(edge.fromNodeIndex());
        NavigationRouteNode to = route.nodes().get(edge.toNodeIndex());
        double ax = from.blockX() + 0.5D;
        double az = from.blockZ() + 0.5D;
        double bx = to.blockX() + 0.5D;
        double bz = to.blockZ() + 0.5D;
        double lenSqr = Math.max(0.0001D, (bx - ax) * (bx - ax) + (bz - az) * (bz - az));
        double progress = ((mob.getX() - ax) * (bx - ax) + (mob.getZ() - az) * (bz - az)) / lenSqr;
        return edge.cost() * (1.0D - clamp(progress, 0.0D, 1.0D));
    }

    private boolean door(NavigationTraversalType type) {
        return type == NavigationTraversalType.OPEN_DOOR || type == NavigationTraversalType.PASS_DOOR;
    }

    private boolean ladder(NavigationTraversalType type) {
        return type == NavigationTraversalType.LADDER_UP || type == NavigationTraversalType.LADDER_DOWN;
    }

    private double horizontalTolerance(NavigationTraversalType type) {
        return switch (type) {
            case STAIR_UP, STAIR_DOWN, SLAB_UP, SLAB_DOWN, STEP_UP, STEP_DOWN, JUMP -> TERRAIN_TOLERANCE;
            case LADDER_UP, LADDER_DOWN -> LADDER_TOLERANCE;
            default -> NODE_TOLERANCE;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Tick(Status status, Vector3 currentPosition, double remainingDistance, int pathIndex, String message) {
    }

    public enum Status {
        MOVING,
        ARRIVED,
        REPLAN_REQUESTED
    }
}
