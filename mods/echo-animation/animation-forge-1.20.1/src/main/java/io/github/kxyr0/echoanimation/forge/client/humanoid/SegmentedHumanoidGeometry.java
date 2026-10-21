package io.github.kxyr0.echoanimation.forge.client.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Shared pixel layout and low-level cuboid drawing for the segmented humanoid. */
final class SegmentedHumanoidGeometry {
    static final float ARM_TOP_Y = -2.0F;
    static final float UPPER_ARM_HEIGHT = 5.0F;
    static final float FOREARM_HEIGHT = 7.0F;
    // Keep the animation skeleton pivot at the original 6 px arm split.
    // The visible mesh is 5 + 7, but the Blender/Echo rig contract still
    // places the forearm joint 6 px from the arm cube top (model Y = 4).
    static final float FOREARM_PIVOT_Y = 4.0F;
    static final float FOREARM_GEOMETRY_Y = ARM_TOP_Y + UPPER_ARM_HEIGHT - FOREARM_PIVOT_Y;
    static final float OVERLAY_INFLATE = 0.25F;
    static final int UPPER_ARM_SOURCE_Y = 0;
    static final int FOREARM_SOURCE_Y = 5;

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float TEXTURE = 64.0F;

    private SegmentedHumanoidGeometry() {}

    record Draw(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay) {}

    static void drawBox(Draw draw, Box box, Uv uv) {
        Points points = points(box);
        float u0 = uv.u();
        float u1 = uv.u() + uv.depth();
        float u2 = u1 + uv.width();
        float u3 = u2 + uv.width();
        float u4 = u2 + uv.depth();
        float u5 = u4 + uv.width();
        float v0 = uv.v();
        float v1 = uv.v() + uv.depth();
        float v2 = v1 + uv.height();
        float splitV0 = v1 + uv.sourceY();
        float splitV1 = splitV0 + uv.visibleHeight();
        FaceUv topUv = uv.topCut() ? cutFaceUv(uv, uv.sourceY()) : new FaceUv(u1, v0, u2, v1);
        FaceUv bottomUv = uv.bottomCut() ? cutFaceUv(uv, uv.sourceY() + uv.visibleHeight() - 1.0F)
                        : new FaceUv(u2, v1, u3, v0);
        face( draw, new Vertex[] {
                    points.px1y0z1(), points.px0y0z1(), points.px0y0z0(), points.px1y0z0()
                }, topUv, new Normal(0.0F, -1.0F, 0.0F));
        face( draw, new Vertex[] {
                    points.px1y1z0(), points.px0y1z0(), points.px0y1z1(), points.px1y1z1()
                }, bottomUv, new Normal(0.0F, 1.0F, 0.0F));
        face( draw, new Vertex[] {
                    points.px0y0z0(), points.px0y0z1(), points.px0y1z1(), points.px0y1z0()
                }, new FaceUv(u0, splitV0, u1, splitV1), new Normal(-1.0F, 0.0F, 0.0F));
        face( draw, new Vertex[] {
                    points.px1y0z0(), points.px0y0z0(), points.px0y1z0(), points.px1y1z0()
                }, new FaceUv(u1, splitV0, u2, splitV1), new Normal(0.0F, 0.0F, -1.0F));
        face( draw, new Vertex[] {
                    points.px1y0z1(), points.px1y0z0(), points.px1y1z0(), points.px1y1z1()
                }, new FaceUv(u2, splitV0, u4, splitV1), new Normal(1.0F, 0.0F, 0.0F));
        face( draw, new Vertex[] {
                    points.px0y0z1(), points.px1y0z1(), points.px1y1z1(), points.px0y1z1()
                }, new FaceUv(u4, splitV0, u5, splitV1), new Normal(0.0F, 0.0F, 1.0F));
        if (v2 < splitV1) {
            throw new IllegalStateException("split UV exceeds cuboid height.");
        }
    }

    private static FaceUv cutFaceUv(Uv uv, float sourceRow) {
        float sideU0 = uv.u() + uv.depth();
        float sideV0 = uv.v() + uv.depth() + sourceRow;
        return new FaceUv(sideU0, sideV0, sideU0 + uv.width(), sideV0 + 1.0F);
    }

    private static Points points(Box box) {
        float x1 = box.x() + box.width();
        float y1 = box.y() + box.height();
        float z1 = box.z() + box.depth();
        return new Points( new Vertex(box.x(), box.y(), box.z()), new Vertex(x1, box.y(), box.z()),
                new Vertex(x1, y1, box.z()), new Vertex(box.x(), y1, box.z()), new Vertex(box.x(), box.y(), z1),
                new Vertex(x1, box.y(), z1), new Vertex(x1, y1, z1), new Vertex(box.x(), y1, z1));
    }

    private static void face(Draw draw, Vertex[] corners, FaceUv uv, Normal normal) {
        vertex(draw, corners[0], new TexturePoint(uv.u1(), uv.v0()), normal);
        vertex(draw, corners[1], new TexturePoint(uv.u0(), uv.v0()), normal);
        vertex(draw, corners[2], new TexturePoint(uv.u0(), uv.v1()), normal);
        vertex(draw, corners[3], new TexturePoint(uv.u1(), uv.v1()), normal);
    }

    private static void vertex(Draw draw, Vertex vertex, TexturePoint texture, Normal normal) {
        draw.vertices() .vertex( draw.pose().pose(), vertex.x() * PIXEL, vertex.y() * PIXEL, vertex.z() * PIXEL)
                .color(255, 255, 255, 255) .uv(texture.u() / TEXTURE, texture.v() / TEXTURE)
                .overlayCoords(draw.overlay()) .uv2(draw.light())
                .normal(draw.pose().normal(), normal.x(), normal.y(), normal.z()) .endVertex();
    }

    record Dimensions(float width, float height, float depth) { }

    record Box(float x, float y, float z, float width, float height, float depth) {}

    record Uv( float u, float v, float width, float height, float depth, float sourceY, float visibleHeight,
            boolean topCut, boolean bottomCut) {
        static Uv visible(float u, float v, Dimensions size, float sourceY, float visibleHeight) {
            return new Uv(u, v, size.width(), size.height(), size.depth(), sourceY, visibleHeight, false, false);
        }

        Uv withTopCut() {
            return new Uv(u, v, width, height, depth, sourceY, visibleHeight, true, bottomCut);
        }

        Uv withBottomCut() {
            return new Uv(u, v, width, height, depth, sourceY, visibleHeight, topCut, true);
        }
    }

    private record FaceUv(float u0, float v0, float u1, float v1) {}

    private record TexturePoint(float u, float v) {}

    private record Normal(float x, float y, float z) {}

    private record Vertex(float x, float y, float z) {}

    private record Points( Vertex px0y0z0, Vertex px1y0z0, Vertex px1y1z0, Vertex px0y1z0, Vertex px0y0z1,
            Vertex px1y0z1, Vertex px1y1z1, Vertex px0y1z1) {}
}
