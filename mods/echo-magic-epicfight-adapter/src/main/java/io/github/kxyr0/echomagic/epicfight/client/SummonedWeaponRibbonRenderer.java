package io.github.kxyr0.echomagic.epicfight.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echomagic.forge.api.client.beam.MagicRibbonMesh;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

final class SummonedWeaponRibbonRenderer {
    private SummonedWeaponRibbonRenderer() { }

    static void render(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers,
                       SummonedWeaponGeometry.Shape shape, int color, double time) {
        VertexConsumer vertices = buffers.getBuffer(MagicRibbonMesh.TYPE);
        for (SummonedWeaponGeometry.Path path : shape.corePaths()) {
            MagicRibbonMesh.path(vertices, event.getPoseStack().last(), event.getCamera(),
                    smooth(path.points()), color, time, 0.034D, 1.0D);
        }
        // These paths define actual guards, tips and knuckles, not crossed copies of a beam.
        for (SummonedWeaponGeometry.Path path : shape.accentPaths()) {
            MagicRibbonMesh.path(vertices, event.getPoseStack().last(), event.getCamera(),
                    smooth(path.points()), color, time, 0.026D, 0.90D);
        }
    }

    static void finish(MultiBufferSource.BufferSource buffers) { buffers.endBatch(MagicRibbonMesh.TYPE); }

    private static List<Vec3> smooth(List<Vec3> source) {
        if (source.size() < 3) return source;
        List<Vec3> result = new ArrayList<>((source.size() - 1) * 2 + 1);
        for (int index = 0; index < source.size() - 1; index++) {
            Vec3 a = source.get(Math.max(0, index - 1)); Vec3 b = source.get(index);
            Vec3 c = source.get(index + 1); Vec3 d = source.get(Math.min(source.size() - 1, index + 2));
            result.add(catmull(a, b, c, d, 0.0D)); result.add(catmull(a, b, c, d, 0.5D));
        }
        result.add(source.get(source.size() - 1)); return result;
    }

    private static Vec3 catmull(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double t2 = t * t; double t3 = t2 * t;
        return new Vec3(spline(a.x, b.x, c.x, d.x, t, t2, t3),
                spline(a.y, b.y, c.y, d.y, t, t2, t3), spline(a.z, b.z, c.z, d.z, t, t2, t3));
    }

    private static double spline(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5D * (2.0D * b + (-a + c) * t + (2.0D * a - 5.0D * b + 4.0D * c - d) * t2
                + (-a + 3.0D * b - 3.0D * c + d) * t3);
    }
}
