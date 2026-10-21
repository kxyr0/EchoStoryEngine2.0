package io.github.kxyr0.echodebug.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echodebug.core.render.RenderLabel;
import io.github.kxyr0.echodebug.core.render.RenderLine;
import io.github.kxyr0.echodebug.core.render.TriggerAreaRenderPlan;
import io.github.kxyr0.echodebug.forge.client.render.TriggerWorldOverlayRenderer.ColoredPlan;
import io.github.kxyr0.echodebug.forge.client.render.TriggerWorldOverlayRenderer.OverlayColor;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;

final class VanillaTriggerOverlayDrawer {
    void draw(PoseStack poses, Vector3 camera, List<ColoredPlan> plans) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        poses.pushPose();
        poses.translate(-camera.X(), -camera.Y(), -camera.Z());
        drawFills(buffers.getBuffer(RenderType.debugQuads()), poses.last(), plans);
        buffers.endBatch(RenderType.debugQuads());
        drawEdges(buffers.getBuffer(RenderType.lines()), poses.last(), plans);
        buffers.endBatch(RenderType.lines());
        poses.popPose();
        drawLabels(poses, buffers, plans);
        buffers.endBatch();
    }

    private static void drawFills(VertexConsumer vertices, PoseStack.Pose pose, List<ColoredPlan> plans) {
        for (ColoredPlan colored : plans) {
            List<RenderLine> lines = colored.plan().lines();
            for (int index = 0; index + 3 < lines.size(); index += 4) {
                drawSquare(vertices, pose, lines.subList(index, index + 4), colored.color());
            }
        }
    }

    private static void drawSquare(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            List<RenderLine> lines,
            OverlayColor color
    ) {
        fillVertex(vertices, pose, lines.get(0).from(), color);
        fillVertex(vertices, pose, lines.get(0).to(), color);
        fillVertex(vertices, pose, lines.get(1).to(), color);
        fillVertex(vertices, pose, lines.get(2).to(), color);
    }

    private static void fillVertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vector3 point,
            OverlayColor color
    ) {
        vertices.vertex(pose.pose(), (float) point.X(), (float) point.Y(), (float) point.Z())
                .color(color.red(), color.green(), color.blue(), color.fillAlpha()).endVertex();
    }

    private static void drawEdges(VertexConsumer vertices, PoseStack.Pose pose, List<ColoredPlan> plans) {
        for (ColoredPlan colored : plans) {
            for (RenderLine line : colored.plan().lines()) {
                drawLine(vertices, pose, line, colored.color());
            }
        }
    }

    private static void drawLine(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            RenderLine line,
            OverlayColor color
    ) {
        Vector3 direction = line.to().subtract(line.from());
        double length = Math.sqrt(direction.X() * direction.X()
                + direction.Y() * direction.Y() + direction.Z() * direction.Z());
        if (length <= 0.0D) {
            return;
        }
        Vector3 normal = direction.divide(length);
        edgeVertex(vertices, pose, line.from(), color, normal);
        edgeVertex(vertices, pose, line.to(), color, normal);
    }

    private static void edgeVertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vector3 point,
            OverlayColor color,
            Vector3 normal
    ) {
        vertices.vertex(pose.pose(), (float) point.X(), (float) point.Y(), (float) point.Z())
                .color(color.red(), color.green(), color.blue(), 255)
                .normal(pose.normal(), (float) normal.X(), (float) normal.Y(), (float) normal.Z()).endVertex();
    }

    private static void drawLabels(
            PoseStack poses,
            MultiBufferSource.BufferSource buffers,
            List<ColoredPlan> plans
    ) {
        for (ColoredPlan colored : plans) {
            for (RenderLabel label : colored.plan().labels()) {
                OverlayColor color = colored.color();
                int packed = 0xFF000000 | color.red() << 16 | color.green() << 8 | color.blue();
                Vector3 point = label.position();
                DebugRenderer.renderFloatingText(poses, buffers, label.text(), point.X(), point.Y(), point.Z(),
                        packed, 0.02F, true, 0.0F, true);
            }
        }
    }
}
