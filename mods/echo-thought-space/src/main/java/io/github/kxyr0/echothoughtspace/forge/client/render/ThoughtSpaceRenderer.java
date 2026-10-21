package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasTransform;
import io.github.kxyr0.echothoughtspace.core.canvas.ThoughtNodeHitTester;
import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionTrace;
import io.github.kxyr0.echothoughtspace.forge.client.input.ThoughtConnectionVisual;
import io.github.kxyr0.echothoughtspace.forge.client.style.ThoughtSpaceStyleConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ThoughtSpaceRenderer {
    private final ThoughtNodeRenderer nodeRenderer = new ThoughtNodeRenderer();
    private final ThoughtConnectionRenderer connectionRenderer = new ThoughtConnectionRenderer();
    private final ThoughtNodeHitTester hitTester = new ThoughtNodeHitTester();
    private List<CanvasPoint> activePath = List.of();
    private List<Box> activeBounds = List.of();

    public void render(GuiGraphics graphics, CanvasTransform transform, CanvasPoint pointer,
            List<ThoughtCanvasNode> nodes, double opacity, double deltaSeconds,
            ThoughtSpaceStyleConfig style, List<ThoughtCombinationRecipe> unlocked,
            Optional<ThoughtConnectionVisual> connection, boolean debug,
            Map<ThoughtNodeId, ThoughtConnectionTrace> traces) {
        int blackAlpha = alpha(255.0D * opacity);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), blackAlpha << 24);
        if (opacity <= 0.001D) return;
        Optional<ThoughtNodeId> hovered = hitTester.hit(nodes, transform, pointer, style.circleRadius())
                .map(value -> value.nodeId());
        graphics.pose().pushPose();
        graphics.pose().scale(transform.viewport().physicalToGuiScaleX(),
                transform.viewport().physicalToGuiScaleY(), 1.0F);
        var entries = nodeRenderer.prepare(transform, nodes, hovered, style, deltaSeconds, traces);
        List<Box> bounds = entries.stream().map(ThoughtNodeRenderer.Entry::bounds).toList();
        activeBounds = bounds;
        drawUnlockedConnections(graphics, transform, nodes, unlocked, style.circleRadius(), opacity, bounds, traces);
        connection.ifPresent(value -> drawConnection(graphics, transform, value, opacity, bounds));
        if (nodes.isEmpty()) drawEmpty(graphics, transform, style, opacity);
        else nodeRenderer.draw(graphics, entries, opacity);
        if (debug) drawDebug(graphics, transform, nodes.size(), opacity);
        graphics.pose().popPose();
        drawTitle(graphics, style, opacity);
    }

    private void drawConnection(GuiGraphics graphics, CanvasTransform transform,
            ThoughtConnectionVisual connection, double opacity, List<Box> bounds) {
        CanvasPoint start = transform.toPhysical(connection.source());
        CanvasPoint end = transform.toPhysical(connection.endpoint());
        int rgb = connection.validTarget() ? 0xf7f9ff : 0xaab0bc;
        int lineAlpha = alpha(opacity * connection.opacity()
                * (connection.validTarget() ? 245.0D : 165.0D));
        activePath = connectionRenderer.route(start, end, bounds, 18 * transform.scale(), List.of());
        connectionRenderer.drawPath(graphics, activePath, lineAlpha << 24 | rgb);
    }

    public List<CanvasPoint> acceptedPath(CanvasTransform transform, CanvasPoint start, CanvasPoint end) {
        var physical = connectionRenderer.route(transform.toPhysical(start), transform.toPhysical(end),
                activeBounds, 18 * transform.scale(), activePath);
        return physical.size() < 2 ? List.of(start, end) : physical.stream().map(transform::toCanvas).toList();
    }

    private void drawUnlockedConnections(GuiGraphics graphics, CanvasTransform transform,
            List<ThoughtCanvasNode> nodes, List<ThoughtCombinationRecipe> unlocked,
            double circleRadius, double opacity, List<Box> bounds, Map<ThoughtNodeId, ThoughtConnectionTrace> traces) {
        Map<ThoughtNodeId, CanvasPoint> positions = new HashMap<>();
        nodes.forEach(node -> positions.put(node.snapshot().id(), transform.toPhysical(node.position())));
        double inset = circleRadius * transform.scale();
        for (ThoughtCombinationRecipe recipe : unlocked) {
            ThoughtConnectionTrace trace = traces.get(recipe.result());
            if (trace != null) {
                drawTrace(graphics, transform, trace.first(), opacity, bounds);
                drawTrace(graphics, transform, trace.second(), opacity, bounds);
                continue;
            }
            CanvasPoint result = positions.get(recipe.result());
            drawGraphEdge(graphics, positions.get(recipe.inputs().first()), result, inset, opacity, bounds, transform.scale());
            drawGraphEdge(graphics, positions.get(recipe.inputs().second()), result, inset, opacity, bounds, transform.scale());
        }
    }

    private void drawTrace(GuiGraphics graphics, CanvasTransform transform, List<CanvasPoint> trace,
            double opacity, List<Box> bounds) {
        var preferred = trace.stream().map(transform::toPhysical).toList();
        if (preferred.size() < 2) return;
        var path = connectionRenderer.route(preferred.get(0), preferred.get(preferred.size() - 1),
                bounds, 18 * transform.scale(), preferred);
        connectionRenderer.drawPath(graphics, path, alpha(opacity * 185) << 24 | 0xd8dde8);
    }

    private void drawGraphEdge(GuiGraphics graphics, CanvasPoint from, CanvasPoint to,
            double inset, double opacity, List<Box> bounds, double scale) {
        if (from == null || to == null) return;
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= inset * 2.0D) return;
        CanvasPoint start = new CanvasPoint(from.x() + dx / length * inset, from.y() + dy / length * inset);
        CanvasPoint end = new CanvasPoint(to.x() - dx / length * inset, to.y() - dy / length * inset);
        connectionRenderer.draw(graphics, start, end, alpha(opacity * 185.0D) << 24 | 0xd8dde8, bounds, 18 * scale);
    }

    private static void drawEmpty(GuiGraphics graphics, CanvasTransform transform,
            ThoughtSpaceStyleConfig style, double opacity) {
        var font = Minecraft.getInstance().font;
        Component text = Component.translatable("screen.echo_thought_space.empty");
        CanvasPoint center = transform.toPhysical(new CanvasPoint(0.0D, 0.0D));
        graphics.pose().pushPose();
        graphics.pose().translate(center.x(), center.y(), 0.0D);
        float scale = (float) (transform.scale() * style.nodeFontSize());
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2,
                alpha(opacity * 185.0D) << 24 | 0xe5e7eb, false);
        graphics.pose().popPose();
    }

    private static void drawTitle(GuiGraphics graphics, ThoughtSpaceStyleConfig style, double opacity) {
        if (!style.titleVisible()) return;
        var font = Minecraft.getInstance().font;
        Component title = Component.literal(style.titleText());
        double x = graphics.guiWidth() * style.titleX();
        double y = graphics.guiHeight() * style.titleY();
        float scale = (float) style.titleSize();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, title, -font.width(title) / 2, -font.lineHeight / 2,
                alpha(opacity * 235.0D) << 24 | 0xf7f9ff, false);
        graphics.pose().popPose();
    }

    private static void drawDebug(GuiGraphics graphics, CanvasTransform transform, int count, double opacity) {
        var font = Minecraft.getInstance().font;
        String value = "nodes=" + count + " camera=" + format(transform.camera().center().x()) + ","
                + format(transform.camera().center().y()) + " fb=" + transform.viewport().framebufferWidth()
                + "x" + transform.viewport().framebufferHeight() + " zoom="
                + format(transform.camera().zoom());
        graphics.drawString(font, Component.literal(value), 10, 10,
                alpha(opacity * 220.0D) << 24 | 0x9fb9ff, false);
    }

    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }
    private static int alpha(double value) { return Math.max(0, Math.min(255, (int) Math.round(value))); }
}
