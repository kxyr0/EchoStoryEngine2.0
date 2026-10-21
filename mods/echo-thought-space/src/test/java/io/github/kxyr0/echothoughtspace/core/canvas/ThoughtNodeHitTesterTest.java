package io.github.kxyr0.echothoughtspace.core.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ThoughtNodeHitTesterTest {
    @Test
    void overlapUsesNearestThenLatestDrawOrder() {
        ThoughtCanvasNode first = node("first", -2.0D);
        ThoughtCanvasNode second = node("second", 2.0D);
        CanvasTransform transform = CanvasTransform.fit(new CanvasCamera(),
                new CanvasViewport(1920, 1080, 960, 540));
        CanvasPoint center = new CanvasPoint(960.0D, 540.0D);
        var hit = new ThoughtNodeHitTester().hit(List.of(first, second), transform, center, 30.0D).orElseThrow();
        assertEquals(new ThoughtNodeId("second"), hit.nodeId());
        assertEquals(1, hit.drawOrder());
    }

    private static ThoughtCanvasNode node(String id, double x) {
        return new ThoughtCanvasNode(new ThoughtNodeSnapshot(new ThoughtNodeId(id),
                new ThoughtNodeLabel(id), new ThoughtNodeRevision(1L)), new ThoughtCanvasPosition(x, 0.0D));
    }
}
