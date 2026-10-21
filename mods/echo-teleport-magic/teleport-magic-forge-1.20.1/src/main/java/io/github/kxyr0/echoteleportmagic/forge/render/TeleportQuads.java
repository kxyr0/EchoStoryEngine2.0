package io.github.kxyr0.echoteleportmagic.forge.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Reused camera basis and direct vertices: no per-orb Vector/Pose allocation. */
public final class TeleportQuads {
    public final Vector3f right = new Vector3f(), up = new Vector3f();
    public VertexConsumer vertices;
    public Matrix4f matrix;

    public void billboard(double x, double y, double z, float size, float red, float green, float blue, float alpha, int tile) {
        quad(x, y, z, right.x * size, right.y * size, right.z * size,
                up.x * size, up.y * size, up.z * size, red, green, blue, alpha, tile);
    }
    public void ground(double x, double y, double z, float size, float red, float green, float blue, float alpha) {
        quad(x, y + .012, z, size, 0, 0, 0, 0, size, red, green, blue, alpha, 3);
    }
    private void quad(double x, double y, double z, float rx, float ry, float rz,
            float ux, float uy, float uz, float red, float green, float blue, float alpha, int tile) {
        float u0 = tile * .2f, u1 = u0 + .2f;
        vertex(x - rx - ux, y - ry - uy, z - rz - uz, red, green, blue, alpha, u0, 1);
        vertex(x + rx - ux, y + ry - uy, z + rz - uz, red, green, blue, alpha, u1, 1);
        vertex(x + rx + ux, y + ry + uy, z + rz + uz, red, green, blue, alpha, u1, 0);
        vertex(x - rx + ux, y - ry + uy, z - rz + uz, red, green, blue, alpha, u0, 0);
    }
    private void vertex(double x, double y, double z, float r, float g, float b, float a, float u, float v) {
        vertices.vertex(matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30(),
                matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31(),
                matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32())
                .color(r, g, b, Math.max(0, Math.min(1, a))).uv(u, v).endVertex();
    }
}
