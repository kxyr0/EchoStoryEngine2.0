package io.github.kxyr0.echodebug.forge.client.trail;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echodebug.core.trail.NpcMovementTrail;
import io.github.kxyr0.echodebug.core.trail.NpcTrailPoint;
import io.github.kxyr0.echodebug.forge.config.EchoDebugConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class NpcTrailRenderer {
    private static final double Y_OFFSET = 0.05D;
    private final NpcTrailClientState trails;

    public NpcTrailRenderer(NpcTrailClientState trails) {
        this.trails = trails;
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active(minecraft) || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        String dimension = minecraft.level.dimension().location().toString();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
        PoseStack poses = event.getPoseStack();
        int trailsInDimension = 0;
        int trailsCulled = 0;
        int submittedSegments = 0;
        RenderSystem.lineWidth(EchoDebugConfig.trailLineWidth());
        poses.pushPose();
        translateToWorld(poses, camera);
        for (NpcMovementTrail trail : trails.renderPlan().trails()) {
            if (!trail.dimension().equals(dimension)) {
                continue;
            }
            trailsInDimension++;
            int segments = drawIfVisible(poses.last(), vertices, trail, camera);
            submittedSegments += segments;
            if (segments == 0) {
                trailsCulled++;
            }
        }
        poses.popPose();
        RenderSystem.lineWidth(1.0F);
        buffers.endBatch(RenderType.lines());
        trails.recordRender(trailsInDimension, trailsCulled, submittedSegments, minecraft.level.getGameTime());
    }

    private static boolean active(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && EchoDebugConfig.showNpcMovementTrails();
    }

    private static int drawIfVisible(PoseStack.Pose pose, VertexConsumer vertices, NpcMovementTrail trail,
            Vec3 camera) {
        if (trail.points().size() < 2 || !nearCamera(trail, camera)) {
            return 0;
        }
        float alpha = EchoDebugConfig.trailOpacity(trail.active());
        List<NpcTrailPoint> points = trail.points();
        for (int index = 1; index < points.size(); index++) {
            vertex(pose, vertices, points.get(index - 1), trail.active(), alpha);
            vertex(pose, vertices, points.get(index), trail.active(), alpha);
        }
        return points.size() - 1;
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, NpcTrailPoint point, boolean active,
            float alpha) {
        int red = active ? 255 : 128;
        int green = active ? 196 : 180;
        int blue = active ? 64 : 255;
        vertices.vertex(pose.pose(), (float) point.x(), (float) (point.y() + Y_OFFSET), (float) point.z())
                .color(red, green, blue, (int) (alpha * 255.0F))
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void translateToWorld(PoseStack poses, Vec3 camera) {
        poses.translate(-camera.x, -camera.y, -camera.z);
    }

    private static boolean nearCamera(NpcMovementTrail trail, Vec3 camera) {
        Bounds bounds = Bounds.from(trail.points());
        double maxDistance = EchoDebugConfig.npcTrailRenderDistance();
        return bounds.distanceSquared(camera) <= maxDistance * maxDistance;
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        static Bounds from(List<NpcTrailPoint> points) {
            Bounds bounds = new Bounds(points.get(0).x(), points.get(0).y(), points.get(0).z(),
                    points.get(0).x(), points.get(0).y(), points.get(0).z());
            for (int index = 1; index < points.size(); index++) {
                bounds = bounds.include(points.get(index));
            }
            return bounds;
        }

        Bounds include(NpcTrailPoint point) {
            return new Bounds(Math.min(minX, point.x()), Math.min(minY, point.y()), Math.min(minZ, point.z()),
                    Math.max(maxX, point.x()), Math.max(maxY, point.y()), Math.max(maxZ, point.z()));
        }

        double distanceSquared(Vec3 point) {
            double dx = axisDistance(point.x, minX, maxX);
            double dy = axisDistance(point.y, minY, maxY);
            double dz = axisDistance(point.z, minZ, maxZ);
            return dx * dx + dy * dy + dz * dz;
        }

        private static double axisDistance(double value, double min, double max) {
            return value < min ? min - value : value > max ? value - max : 0.0D;
        }
    }
}
