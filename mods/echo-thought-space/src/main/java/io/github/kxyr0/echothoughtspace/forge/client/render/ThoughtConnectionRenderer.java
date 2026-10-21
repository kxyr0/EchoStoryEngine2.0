package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRoute.Box;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionRouter;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionMesh;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

public final class ThoughtConnectionRenderer {
    private final ThoughtConnectionRouter router = new ThoughtConnectionRouter();
    private final Map<Key, List<CanvasPoint>> cache = new HashMap<>();

    public void draw(GuiGraphics graphics, CanvasPoint start, CanvasPoint end, int color,
            List<Box> labels, double bendRadius) {
        drawPath(graphics, route(start, end, labels, bendRadius, List.of()), color);
    }

    public List<CanvasPoint> route(CanvasPoint start, CanvasPoint end, List<Box> labels,
            double bendRadius, List<CanvasPoint> preferred) {
        if (cache.size() > 128) cache.clear();
        return cache.computeIfAbsent(new Key(start, end, labels, bendRadius, preferred),
                key -> router.route(key.start, key.end, key.labels, key.radius, key.preferred));
    }

    public void drawPath(GuiGraphics graphics, List<CanvasPoint> path, int color) {
        if (path.size() < 2) return;
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        var matrix = graphics.pose().last().pose();
        for (var quad : ThoughtConnectionMesh.build(path)) {
            for (CanvasPoint point : List.of(quad.first(), quad.second(), quad.third(), quad.fourth())) {
                buffer.vertex(matrix, (float) point.x(), (float) point.y(), 0).color(color).endVertex();
            }
        }
        graphics.flush();
    }

    private record Key(CanvasPoint start, CanvasPoint end, List<Box> labels, double radius, List<CanvasPoint> preferred) { }
}
