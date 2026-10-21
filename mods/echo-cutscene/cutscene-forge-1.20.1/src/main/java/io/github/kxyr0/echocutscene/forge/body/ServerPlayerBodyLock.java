package io.github.kxyr0.echocutscene.forge.body;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ServerPlayerBodyLock {
    private static final double MAX_DRIFT = 0.015D;
    private static final double MAX_ROTATION_DRIFT = 0.01D;
    private final BodyLockStore locks;

    public ServerPlayerBodyLock(BodyLockStore locks) {
        this.locks = locks;
    }

    public void enforce(ServerPlayer player) {
        locks.snapshot(player).ifPresent(snapshot -> restoreIfNeeded(player, snapshot));
    }

    public boolean locked(ServerPlayer player) {
        return locks.locked(player);
    }

    private void restoreIfNeeded(ServerPlayer player, PlayerBodySnapshot snapshot) {
        restoreStaticState(player, snapshot);
        if (snapshot.vehicleId().isPresent() && restoreVehicle(player, snapshot)) {
            restoreRotation(player, snapshot);
            restorePosition(player, snapshot);
            return;
        }
        restorePosition(player, snapshot);
        restoreRotation(player, snapshot);
    }

    private boolean restoreVehicle(ServerPlayer player, PlayerBodySnapshot snapshot) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null && snapshot.vehicleId().orElseThrow().equals(vehicle.getUUID())) {
            return true;
        }
        Entity savedVehicle = player.serverLevel().getEntity(snapshot.vehicleId().orElseThrow());
        return savedVehicle != null && player.startRiding(savedVehicle, true);
    }

    private void restoreStaticState(ServerPlayer player, PlayerBodySnapshot snapshot) {
        player.setShiftKeyDown(false);
        player.setSprinting(false);
        player.setJumping(false);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.resetFallDistance();
        player.setPose(snapshot.pose());
        player.getInventory().selected = snapshot.selectedSlot();
    }

    private void restorePosition(ServerPlayer player, PlayerBodySnapshot snapshot) {
        if (bodyDrifted(player, snapshot)) {
            player.connection.teleport(snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
        }
    }

    private void restoreRotation(ServerPlayer player, PlayerBodySnapshot snapshot) {
        player.setYRot(snapshot.yaw());
        player.setXRot(snapshot.pitch());
        player.setYHeadRot(snapshot.yaw());
        player.yHeadRotO = snapshot.yaw();
        player.yBodyRot = snapshot.yaw();
        player.yBodyRotO = snapshot.yaw();
    }

    private static boolean bodyDrifted(ServerPlayer player, PlayerBodySnapshot snapshot) {
        return distanceSquared(player, snapshot) > MAX_DRIFT * MAX_DRIFT
                || player.getPose() != snapshot.pose()
                || rotationDrifted(player, snapshot);
    }

    private static double distanceSquared(ServerPlayer player, PlayerBodySnapshot snapshot) {
        double dx = player.getX() - snapshot.x();
        double dy = player.getY() - snapshot.y();
        double dz = player.getZ() - snapshot.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean rotationDrifted(ServerPlayer player, PlayerBodySnapshot snapshot) {
        return Math.abs(wrapDegrees(player.getYRot() - snapshot.yaw())) > MAX_ROTATION_DRIFT
                || Math.abs(player.getXRot() - snapshot.pitch()) > MAX_ROTATION_DRIFT;
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }
}
