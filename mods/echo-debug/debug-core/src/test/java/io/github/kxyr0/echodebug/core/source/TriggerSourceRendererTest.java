package io.github.kxyr0.echodebug.core.source;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodebug.core.path.TriggerIdPlanner;
import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionGeometry;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriggerSourceRendererTest {
    private final TriggerSourceRenderer renderer = new TriggerSourceRenderer();

    @Test
    void rendersDeterministicBoxSource() {
        String source = render(TriggerWandShapeMode.BOX, List.of(point(205, 8, 205), point(205, 8, 205)));

        assertTrue(source.contains("class HouseExit : TriggerResource"));
        assertTrue(source.contains("Id(\"jdh:chapter_1/house_exit\")"));
        assertTrue(source.contains("BoxArea(Vector3(205, 8, 205), Vector3(206, 9, 206))"));
        assertFalse(source.contains("D:\\"));
    }

    @Test
    void rendersPolygonWithExplicitMinMaxY() {
        String source = render(TriggerWandShapeMode.POLYGON, List.of(
                point(0, 64, 0), point(4, 64, 0), point(4, 64, 4), point(0, 64, 4)));

        assertTrue(source.contains("PolygonArea(points: [Vector3(0, 64, 0)"));
        assertTrue(source.contains("minY: 64, maxY: 67"));
        assertTrue(source.getBytes(StandardCharsets.UTF_8).length > 0);
    }

    private String render(TriggerWandShapeMode shape, List<TriggerBlockPoint> points) {
        var id = new TriggerIdPlanner().plan("jdh", "chapter_1/house_exit").value().orElseThrow();
        var geometry = TriggerSelectionGeometry.from(shape, points).orElseThrow();
        return renderer.render(TriggerSourceModel.player(id, "minecraft:overworld", shape, geometry));
    }

    private static TriggerBlockPoint point(int x, int y, int z) {
        return new TriggerBlockPoint("minecraft:overworld", x, y, z);
    }
}
