package io.github.kxyr0.echomagic.forge.api.client.beam;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Continuous, soft edged ribbons. All positions are camera relative before projection. */
public final class MagicRibbonMesh extends RenderType {
    public static final RenderType TYPE = create("echo_magic_ribbon", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 4096, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(new ResourceLocation("minecraft", "textures/misc/white.png"), false, false))
                    // Alpha compositing preserves the school hue instead of washing it out.
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));

    private MagicRibbonMesh() {
        super("echo_magic_ribbon", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                4096, false, false, () -> { }, () -> { });
    }

    public static void path(VertexConsumer vertices, PoseStack.Pose pose, Camera camera, List<Vec3> points,
                            int color, double time, double width, double strength) {
        if (points.size() < 2) return;
        int bodyColor = vivid(color);
        double head = time * 0.0384D % 1.0D;
        for (int index = 1; index < points.size(); index++) {
            double fraction = (index - 0.5D) / (points.size() - 1.0D);
            double distance = Math.abs(fraction - head);
            double pulse = Math.exp(-distance * distance / 0.0038D);
            double wa = width * 0.85D * taper((index - 1.0D) / (points.size() - 1.0D)) * (1.0D + 0.075D * Math.sin(time * 0.30D - (index - 1.0D) / (points.size() - 1.0D) * 9.0D));
            double wb = width * 0.85D * taper(index / (points.size() - 1.0D)) * (1.0D + 0.075D * Math.sin(time * 0.30D - index / (points.size() - 1.0D) * 9.0D));
            Vec3 a = points.get(index - 1); Vec3 b = points.get(index);
            Vec3 previous = points.get(Math.max(0, index - 2));
            Vec3 next = points.get(Math.min(points.size() - 1, index + 1));
            Vec3 sa = side(b.subtract(previous), a, camera);
            Vec3 sb = side(next.subtract(a), b, camera);
            if (sa == null || sb == null) continue;
            // Shared endpoint normals close each join; there are no separate particle disks.
            strip(vertices, pose, camera, a, b, sa, sb, wa * 1.4D, wb * 1.4D, bodyColor, (0.84D + pulse * 0.10D) * strength, true);
            strip(vertices, pose, camera, a, b, sa, sb, wa * 1.4D * 0.225D, wb * 1.4D * 0.225D,
                    0xFFFFFF, (0.20D + pulse * 0.08D) * strength, false);
        }
    }

    private static int vivid(int color) {
        int r = color >> 16 & 255; int g = color >> 8 & 255; int b = color & 255;
        int max = Math.max(r, Math.max(g, b)); int min = Math.min(r, Math.min(g, b));
        if (max > min && min > max * 0.72D) {
            r = Math.max(0, max - (max - r) * 3);
            g = Math.max(0, max - (max - g) * 3);
            b = Math.max(0, max - (max - b) * 3);
        }
        return r << 16 | g << 8 | b;
    }

    private static Vec3 side(Vec3 direction, Vec3 at, Camera camera) {
        Vec3 side = direction.cross(camera.getPosition().subtract(at));
        if (side.lengthSqr() < 1.0E-8D) side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        return side.lengthSqr() < 1.0E-10D ? null : side.normalize();
    }

    private static void strip(VertexConsumer vertices, PoseStack.Pose pose, Camera camera, Vec3 a, Vec3 b,
                              Vec3 sa, Vec3 sb, double widthA, double widthB, int color, double alpha, boolean soft) {
        int bands = soft ? 6 : 2;
        for (int band = 0; band < bands; band++) {
            double lo = -1.0D + 2.0D * band / bands; double hi = -1.0D + 2.0D * (band + 1) / bands;
            double al = alpha * falloff(lo); double ah = alpha * falloff(hi);
            vertex(vertices, pose, a.add(sa.scale(lo * widthA)).subtract(camera.getPosition()), color, al);
            vertex(vertices, pose, b.add(sb.scale(lo * widthB)).subtract(camera.getPosition()), color, al);
            vertex(vertices, pose, b.add(sb.scale(hi * widthB)).subtract(camera.getPosition()), color, ah);
            vertex(vertices, pose, a.add(sa.scale(hi * widthA)).subtract(camera.getPosition()), color, ah);
        }
    }

    private static double falloff(double across) { return Math.pow(1.0D - across * across, 2.0D); }

    private static double taper(double fraction) {
        double edge = Math.min(fraction, 1.0D - fraction) / 0.12D;
        double t = Math.max(0.0D, Math.min(1.0D, edge));
        return t * t * (3.0D - 2.0D * t);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point, int color, double alpha) {
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (Math.min(1.0D, alpha) * 255.0D))
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
