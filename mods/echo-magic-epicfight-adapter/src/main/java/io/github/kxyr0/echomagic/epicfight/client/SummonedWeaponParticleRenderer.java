package io.github.kxyr0.echomagic.epicfight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Frame-attached circular particles: no stale particle copies during Epic Fight attacks. */
final class SummonedWeaponParticleRenderer {
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(
            new ResourceLocation("echo_magic_epicfight", "textures/particle/weapon_circle.png"));
    private static final RenderType CORE = RenderType.entityTranslucentEmissive(
            new ResourceLocation("echo_magic_epicfight", "textures/particle/weapon_circle_core.png"));
    private SummonedWeaponParticleRenderer() { }

    static void render(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers,
                       SummonedWeaponGeometry.Shape shape, String form, int color, double time) {
        List<Dot> dots = new ArrayList<>();
        switch (form) {
            case "spear" -> {
                tube(dots, shape.corePaths().get(0).points(), event, 0.035D, false);
                triangle(dots, shape.accentPaths().get(0).points(), 10, 20);
            }
            case "dagger" -> {
                triangle(dots, shape.accentPaths().get(0).points(), 11, 22);
                tube(dots, shape.corePaths().get(0).points(), event, 0.023D, true);
            }
            case "blade" -> blade(dots, shape.corePaths());
            case "gauntlets" -> {
                for (SummonedWeaponGeometry.Path path : shape.corePaths()) shell(dots, path.points());
            }
            case "whip" -> tube(dots, shape.corePaths().get(0).points(), event, 0.060D, true);
            default -> tube(dots, shape.corePaths().get(0).points(), event, 0.025D, true);
        }
        if (!form.equals("spear") && !form.equals("dagger")) {
            for (SummonedWeaponGeometry.Path path : shape.accentPaths()) {
                tube(dots, path.points(), event, 0.022D, false);
            }
        }
        // Guards and handles remain distinct from the broad blade/head fill.
        for (Vec3 point : shape.motes()) dots.add(new Dot(point, 0.110F, 0.60F));
        // Large, loosely overlapping lobes keep the circular magic texture visible.
        for (SummonedWeaponGeometry.Path path : shape.corePaths()) {
            fringe(dots, path.points(), event, time);
        }
        for (SummonedWeaponGeometry.Path path : shape.accentPaths()) {
            fringe(dots, path.points(), event, time);
        }
        VertexConsumer vertices = buffers.getBuffer(TYPE);
        int sample = 0;
        for (Dot dot : dots) {
            float breath = (float) (0.98D + 0.02D * Math.sin(time * 0.08D));
            billboard(event, vertices, dot.point(), dot.size() * breath * sizeNoise(sample++), color, dot.alpha());
        }
        // Finish BODY before CORE: quad sorting must not put orange discs over the light.
        buffers.endBatch(TYPE);
        vertices = buffers.getBuffer(CORE);
        sample = 0; for (Dot dot : dots) {
            float variation = sizeNoise(sample++);
            if (dot.alpha() < 0.65F) continue;
            Vec3 towardsCamera = event.getCamera().getPosition().subtract(dot.point()).normalize();
            billboard(event, vertices, dot.point().add(towardsCamera.scale(0.012D)),
                    dot.size() * variation * 0.58F, luminousColor(color), 0.82F);
        }
        buffers.endBatch(CORE);
    }

    private static float sizeNoise(int sample) {
        return (float) (1.12D + 0.15D * Math.sin(sample * 0.47D) + 0.07D * Math.sin(sample * 1.13D));
    }
    private static int luminousColor(int color) {
        return (75 + (color >> 16 & 255) * 3 / 5) << 16
                | (75 + (color >> 8 & 255) * 3 / 5) << 8
                | (75 + (color & 255) * 3 / 5);
    }
    private static void fringe(List<Dot> dots, List<Vec3> path,
                               RenderLevelStageEvent event, double time) {
        double distance = 0.0D, next = 0.0D;
        for (int index = 1; index < path.size(); index++) {
            Vec3 a = path.get(index - 1), delta = path.get(index).subtract(a);
            double length = delta.length();
            Vec3 side = delta.cross(event.getCamera().getPosition().subtract(a));
            if (length < 1.0E-8D || side.lengthSqr() < 1.0E-10D) continue;
            side = side.normalize();
            while (next < distance + length) {
                double along = Math.max(0.0D, (next - distance) / length);
                double progress = (index - 1.0D + along) / (path.size() - 1.0D);
                double taper = smoothSize((1.0D - progress) / 0.35D);
                // Small overlapping edge discs avoid a visible necklace of large rings.
                for (int sign : new int[] {-1, 1}) {
                    Vec3 at = a.add(delta.scale(along)).add(side.scale(sign * 0.015D * taper));
                    dots.add(new Dot(at, (float) (0.028D * taper), 0.18F));
                }
                next += 0.012D * Math.max(0.25D, taper);
            }
            distance += length;
        }
    }
    private static void tube(List<Dot> dots, List<Vec3> path, RenderLevelStageEvent event,
                             double radius, boolean tapered) {
        double distance = 0.0D, next = 0.0D;
        for (int index = 1; index < path.size(); index++) {
            Vec3 a = path.get(index - 1); Vec3 b = path.get(index);
            Vec3 direction = b.subtract(a);
            Vec3 side = direction.cross(event.getCamera().getPosition().subtract(a));
            if (side.lengthSqr() < 1.0E-10D) side = direction.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-10D) continue;
            side = side.normalize();
            double length = direction.length();
            while (next < distance + length) {
                double along = Math.max(0.0D, (next - distance) / length);
                double t = (index - 1.0D + along) / (path.size() - 1.0D);
                double taper = tapered ? smoothSize(1.0D - t) : 1.0D;
                Vec3 at = a.lerp(b, along);
                float size = (float) ((0.105D + 0.017D * Math.sin(next * 27.0D)) * taper);
                dots.add(new Dot(at, size, 1.0F));
                // Overlapping side rows fill the entire shaft with circular particles.
                for (int sign : new int[] {-1, 1}) {
                    Vec3 fill = a.lerp(b, along).add(side.scale(sign * 0.045D * taper));
                    dots.add(new Dot(fill, size * 0.80F, 0.64F));
                }
                next += 0.012D * (tapered ? Math.max(0.22D, taper) : 1.0D);
            }
            distance += length;
        }
    }

    private static void triangle(List<Dot> dots, List<Vec3> edge, int tipIndex, int rightIndex) {
        Vec3 left = edge.get(0); Vec3 tip = edge.get(tipIndex); Vec3 right = edge.get(rightIndex);
        int rows = Math.max(5, (int) Math.ceil(tip.distanceTo(left.add(right).scale(0.5D)) / 0.010D));
        for (int row = 0; row <= rows; row++) {
            double t = row / (double) rows; t *= t;
            Vec3 a = tip.lerp(left, t); Vec3 b = tip.lerp(right, t);
            int columns = Math.max(2, (int) Math.ceil(a.distanceTo(b) / 0.012D));
            columns += columns % 2;
            for (int column = 0; column <= columns; column++) {
                double u = column / (double) columns;
                dot(dots, a.lerp(b, u), u * 2.0D - 1.0D, smoothSize(t / 0.55D));
            }
        }
    }

    private static void blade(List<Dot> dots, List<SummonedWeaponGeometry.Path> paths) {
        List<Vec3> a = paths.get(0).points(); List<Vec3> b = paths.get(1).points();
        for (int index = 1; index < Math.min(a.size(), b.size()); index++) {
            for (int step = 0; step < 4; step++) {
                double t = step / 4.0D;
                Vec3 left = a.get(index - 1).lerp(a.get(index), t);
                Vec3 right = b.get(index - 1).lerp(b.get(index), t);
                int columns = Math.max(2, (int) Math.ceil(left.distanceTo(right) / 0.012D));
                columns += columns % 2;
            for (int column = 0; column <= columns; column++) {
                    double u = column / (double) columns;
                    dot(dots, left.lerp(right, u), u * 2.0D - 1.0D,
                            smoothSize((a.size() - index - t) / 7.0D));
                }
            }
        }
    }

    private static void shell(List<Dot> dots, List<Vec3> ring) {
        Vec3 center = Vec3.ZERO;
        for (int index = 0; index < ring.size() - 1; index++) center = center.add(ring.get(index));
        center = center.scale(1.0D / (ring.size() - 1));
        dots.add(new Dot(center, 0.160F, 0.60F));
        for (int index = 1; index < ring.size(); index++) {
            for (int step = 0; step < 4; step++) {
                Vec3 edge = ring.get(index - 1).lerp(ring.get(index), step / 4.0D);
                for (int layer = 1; layer <= 3; layer++) {
                    double u = layer / 3.0D;
                    dot(dots, center.lerp(edge, u), u, 1.0D);
                }
            }
        }
    }

    private static double smoothSize(double value) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        return 0.08D + 0.92D * t * t * (3.0D - 2.0D * t);
    }
    private static void dot(List<Dot> dots, Vec3 point, double across, double taper) {
        double center = 1.0D - Math.abs(across);
        center = center * center * (3.0D - 2.0D * center);
        float size = (float) ((0.035D + 0.100D * center) * Math.max(0.08D, taper));
        dots.add(new Dot(point, size, (float) (0.64D + 0.36D * center)));
    }

    private static void billboard(RenderLevelStageEvent event, VertexConsumer vertices, Vec3 point,
                                   float size, int color, float alpha) {
        PoseStack poses = event.getPoseStack(); poses.pushPose();
        Vec3 at = point.subtract(event.getCamera().getPosition()); poses.translate(at.x, at.y, at.z);
        poses.mulPose(event.getCamera().rotation());
        PoseStack.Pose pose = poses.last();
        vertex(vertices, pose, -size, -size, 0, 1, color, alpha);
        vertex(vertices, pose, size, -size, 1, 1, color, alpha);
        vertex(vertices, pose, size, size, 1, 0, color, alpha);
        vertex(vertices, pose, -size, size, 0, 0, color, alpha);
        poses.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, int color, float alpha) {
        vertices.vertex(pose.pose(), x, y, 0).color(color >> 16 & 255, color >> 8 & 255,
                color & 255, Math.round(alpha * 255)).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 0, 1).endVertex();
    }
    private record Dot(Vec3 point, float size, float alpha) { }
}
