package io.github.kxyr0.echoteleportmagic.forge.client;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import io.github.kxyr0.echoteleportmagic.visual.TeleportParticleField;
import io.github.kxyr0.echoteleportmagic.visual.TeleportParticleSamples;
import io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public final class ClientTeleportVisual {
    public final TeleportVisualPacket packet;
    public final TeleportParticleField field;
    public final TeleportParticleSamples samples;
    public final double[] originX, originY, originZ;
    public TeleportAnchor anchor;
    public net.minecraft.world.phys.AABB bounds;
    public double groundY;
    public boolean frozen;
    public double freeze = -1;
    private double clockShift;
    private LivingEntity tracked;
    private double groundX = Double.NaN, groundZ, groundAnchorY;
    private long groundTick = Long.MIN_VALUE;
    public int rendered;

    public ClientTeleportVisual(TeleportVisualPacket packet) {
        this.packet = packet;
        setAnchor(packet.anchor());
        field = new TeleportParticleField(packet.seed(), anchor.width(), anchor.height(), packet.arrival());
        samples = new TeleportParticleSamples(field);
        int size = field.particles().size();
        originX = new double[size]; originY = new double[size]; originZ = new double[size];
        java.util.Arrays.fill(originX, Double.NaN);
        frozen = packet.arrival();
        updateGround();
    }
    public double time(float partialTick) {
        if (freeze >= 0) return freeze;
        var level = Minecraft.getInstance().level;
        double elapsed = level == null ? 4 : (level.getGameTime() - packet.startTick() + partialTick) / 20.0;
        if (packet.arrival()) return elapsed;
        if (!packet.preview() && !frozen) return Math.min(TeleportTimeline.VISUAL_COMMIT_SECONDS, TeleportTimeline.visualTime(elapsed));
        return TeleportTimeline.visualTime(elapsed - clockShift);
    }
    public void update() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        double t = time(0);
        if (!frozen && t < 1.85) {
            if (tracked == null || tracked.isRemoved()) for (var entity : level.entitiesForRendering()) {
                if (entity.getUUID().equals(packet.subject()) && entity instanceof LivingEntity living) {
                    tracked = living;
                    break;
                }
            }
            if (tracked != null && !tracked.isRemoved()) {
                if (anchor.x() != tracked.getX() || anchor.y() != tracked.getY() || anchor.z() != tracked.getZ()
                        || anchor.yaw() != tracked.getYRot()) {
                    setAnchor(new TeleportAnchor(tracked.getX(), tracked.getY(), tracked.getZ(), tracked.getYRot(),
                            packet.anchor().width(), packet.anchor().height()));
                }
                updateGround();
            }
        }
        for (int i = 0; i < originX.length; i++) {
            if (Double.isNaN(originX[i]) && field.particles().get(i).birth() <= t + .05) {
                originX[i] = anchor.x(); originY[i] = anchor.y(); originZ[i] = anchor.z();
            }
        }
    }
    public void commit(TeleportAnchor position) {
        var level = Minecraft.getInstance().level;
        if (level != null && !packet.preview()) clockShift = Math.max(0,
                (level.getGameTime() - packet.startTick()) / 20.0 - TeleportTimeline.COMMIT_SECONDS);
        setAnchor(position); frozen = true;
        updateGround();
    }
    private void updateGround() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = mc.level.getGameTime();
        if (groundX == anchor.x() && groundZ == anchor.z() && groundAnchorY == anchor.y()
                && groundTick != Long.MIN_VALUE && now - groundTick < 10) return;
        groundX = anchor.x(); groundZ = anchor.z(); groundAnchorY = anchor.y(); groundTick = now;
        groundY = anchor.y();
        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                new net.minecraft.world.phys.Vec3(anchor.x(), anchor.y() + .15, anchor.z()),
                new net.minecraft.world.phys.Vec3(anchor.x(), anchor.y() - 2.5, anchor.z()),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) groundY = hit.getLocation().y;
    }
    private void setAnchor(TeleportAnchor position) {
        anchor = position;
        float spread = Math.max(anchor.height(), anchor.width()) * 4;
        bounds = new net.minecraft.world.phys.AABB(anchor.x() - spread, anchor.y() - 1, anchor.z() - spread,
                anchor.x() + spread, anchor.y() + spread * 2, anchor.z() + spread);
    }
}
