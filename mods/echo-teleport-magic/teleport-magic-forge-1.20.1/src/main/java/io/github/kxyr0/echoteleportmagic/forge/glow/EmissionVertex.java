package io.github.kxyr0.echoteleportmagic.forge.glow;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Mirrors the already posed vertices: no second animation evaluation or humanoid proxy. */
final class EmissionVertex implements VertexConsumer {
    VertexConsumer base, light;
    float intensity;
    private double x, y, z;
    private float nx, ny, nz;
    private int alpha = 255;

    @Override public VertexConsumer vertex(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z; base.vertex(x, y, z); return this;
    }
    @Override public VertexConsumer color(int r, int g, int b, int a) {
        alpha = a; base.color(r, g, b, a); return this;
    }
    @Override public VertexConsumer uv(float u, float v) { base.uv(u, v); return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { base.overlayCoords(u, v); return this; }
    @Override public VertexConsumer uv2(int u, int v) { base.uv2(u, v); return this; }
    @Override public VertexConsumer normal(float x, float y, float z) {
        nx = x; ny = y; nz = z; base.normal(x, y, z); return this;
    }
    @Override public void endVertex() {
        base.endVertex();
        light.vertex(x, y, z).color(1, .88f, 0, intensity * alpha / 255f).uv(.5f, .5f)
                .overlayCoords(0, 10).uv2(240, 240).normal(nx, ny, nz).endVertex();
    }
    @Override public void defaultColor(int r, int g, int b, int a) { alpha = a; base.defaultColor(r, g, b, a); }
    @Override public void unsetDefaultColor() { base.unsetDefaultColor(); }
}
