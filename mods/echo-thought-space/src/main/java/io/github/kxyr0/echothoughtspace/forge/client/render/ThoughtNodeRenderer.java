package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasTransform;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtLabelLayout;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionTrace;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import io.github.kxyr0.echothoughtspace.forge.client.style.ThoughtSpaceStyleConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

public final class ThoughtNodeRenderer {
    private final ThoughtLabelCache labels = new ThoughtLabelCache();
    private final ThoughtHoverAnimation hover = new ThoughtHoverAnimation();
    private final ThoughtLabelLayout layout = new ThoughtLabelLayout();

    public record Entry(CanvasPoint center, CanvasPoint position, double radius, double emphasis,
            double scale, List<FormattedCharSequence> lines, Box bounds) { }

    public List<Entry> prepare(CanvasTransform transform, List<ThoughtCanvasNode> nodes,
            Optional<ThoughtNodeId> hovered, ThoughtSpaceStyleConfig style, double deltaSeconds,
            Map<ThoughtNodeId, ThoughtConnectionTrace> traces) {
        var font = Minecraft.getInstance().font;
        List<CanvasPoint> circles = nodes.stream().map(node -> transform.toPhysical(node.position())).toList();
        List<Entry> result = new ArrayList<>();
        List<Box> occupied = new ArrayList<>();
        double radius = style.circleRadius() * transform.scale();
        double scale = transform.scale() * style.nodeFontSize();
        double gap = 4.0D * transform.scale() + 2.0D;
        for (ThoughtCanvasNode node : nodes) {
            CanvasPoint center = transform.toPhysical(node.position());
            if (!visible(transform, center, radius)) continue;
            double emphasis = hover.update(node.snapshot().id(), hovered.filter(node.snapshot().id()::equals).isPresent(), deltaSeconds);
            var lines = labels.lines(font, node);
            double width = lines.stream().mapToInt(font::width).max().orElse(0) * scale;
            double height = (lines.size() * (font.lineHeight + 2) - 2) * scale;
            var origin = new CanvasPoint(center.x(), center.y() + (style.circleRadius() + 12) * transform.scale());
            var trace = traces.get(node.snapshot().id());
            if (trace != null && trace.first().size() > 1 && trace.second().size() > 1) {
                var before = trace.first().get(trace.first().size() - 2);
                var after = trace.second().get(1);
                if (Math.abs(after.y() - before.y()) > Math.abs(after.x() - before.x())) {
                    origin = new CanvasPoint(center.x() + width / 2 + radius * 1.12D + gap + 1,
                            center.y() - height / 2);
                }
            }
            CanvasPoint position = layout.place(origin, width, height, gap, center, radius * 1.12D, List.of(), circles, occupied);
            var bounds = new Box(position.x() - width / 2 - gap, position.y() - gap,
                    position.x() + width / 2 + gap, position.y() + height + gap);
            result.add(new Entry(center, position, radius * (1 + emphasis * 0.12D), emphasis, scale, lines, bounds));
            occupied.add(bounds);
        }
        return List.copyOf(result);
    }

    public void draw(GuiGraphics graphics, List<Entry> entries, double opacity) {
        var font = Minecraft.getInstance().font;
        for (Entry entry : entries) {
            ThoughtCircleGeometry.drawFilled(graphics, entry.center, entry.radius,
                    alpha(opacity * (150 + entry.emphasis * 105)) << 24 | 0xf7f9ff);
        }
        for (Entry entry : entries) {
            graphics.pose().pushPose();
            graphics.pose().translate(entry.position.x(), entry.position.y(), 0);
            graphics.pose().scale((float) entry.scale, (float) entry.scale, 1);
            int y = 0, color = alpha(opacity * (205 + entry.emphasis * 50)) << 24 | 0xf7f9ff;
            for (var line : entry.lines) {
                graphics.drawString(font, line, -font.width(line) / 2, y, color, false);
                y += font.lineHeight + 2;
            }
            graphics.pose().popPose();
        }
    }

    private static boolean visible(CanvasTransform transform, CanvasPoint center, double radius) {
        double margin = radius + 140.0D * transform.scale();
        return center.x() >= -margin && center.y() >= -margin
                && center.x() <= transform.viewport().framebufferWidth() + margin
                && center.y() <= transform.viewport().framebufferHeight() + margin;
    }

    private static int alpha(double value) { return Math.max(0, Math.min(255, (int) Math.round(value))); }
}
