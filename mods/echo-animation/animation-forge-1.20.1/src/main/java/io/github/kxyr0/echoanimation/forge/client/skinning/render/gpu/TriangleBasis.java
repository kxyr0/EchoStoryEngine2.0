package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import org.joml.Vector3f;

/** UV midpoint and tangent frame required by Iris/Oculus entity shaders. */
record TriangleBasis(float midU, float midV, float x, float y, float z, float handedness) {
    static TriangleBasis calculate(SkinnedVertex a, SkinnedVertex b, SkinnedVertex c) {
        Vector3f edge1 = difference(b, a);
        Vector3f edge2 = difference(c, a);
        float du1 = b.u() - a.u();
        float dv1 = b.v() - a.v();
        float du2 = c.u() - a.u();
        float dv2 = c.v() - a.v();
        float determinant = du1 * dv2 - du2 * dv1;
        Vector3f normal = new Vector3f(a.normalX(), a.normalY(), a.normalZ()).normalize();
        Vector3f tangent;
        Vector3f bitangent;
        if (Math.abs(determinant) < 0.0000001F) {
            tangent = fallback(normal);
            bitangent = new Vector3f(normal).cross(tangent);
        } else {
            float inverse = 1.0F / determinant;
            tangent = new Vector3f(edge1).mul(dv2).sub(new Vector3f(edge2).mul(dv1))
                    .mul(inverse).normalize();
            bitangent = new Vector3f(edge2).mul(du1).sub(new Vector3f(edge1).mul(du2))
                    .mul(inverse).normalize();
        }
        float handedness = new Vector3f(normal).cross(tangent).dot(bitangent) < 0.0F ? -1.0F : 1.0F;
        float midU = (minimum(a.u(), b.u(), c.u()) + maximum(a.u(), b.u(), c.u())) * 0.5F;
        float midV = (minimum(a.v(), b.v(), c.v()) + maximum(a.v(), b.v(), c.v())) * 0.5F;
        return new TriangleBasis(midU, midV, tangent.x(), tangent.y(), tangent.z(), handedness);
    }

    private static Vector3f difference(SkinnedVertex left, SkinnedVertex right) {
        return new Vector3f(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }

    private static Vector3f fallback(Vector3f normal) {
        Vector3f axis = Math.abs(normal.x()) < 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
        return axis.sub(new Vector3f(normal).mul(axis.dot(normal))).normalize();
    }

    private static float minimum(float a, float b, float c) {
        return Math.min(a, Math.min(b, c));
    }

    private static float maximum(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }
}
