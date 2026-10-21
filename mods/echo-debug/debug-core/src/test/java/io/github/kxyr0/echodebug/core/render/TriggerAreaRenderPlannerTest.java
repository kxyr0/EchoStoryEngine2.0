package io.github.kxyr0.echodebug.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class TriggerAreaRenderPlannerTest {
    private final TriggerAreaRenderPlanner planner = new TriggerAreaRenderPlanner();

    @Test
    void boxPlanContainsOneWorldSpaceSquarePerBlockAndNoParserCalls() {
        var plan = planner.plan(box(), "minecraft:overworld", new Vector3(0, 64, 0), 512).orElseThrow();

        assertEquals(64, plan.lines().size());
        assertEquals(0, plan.sourceParserCalls());
        assertEquals(65.025D, plan.lines().get(0).from().Y(), 0.0001D);
        assertTrue(plan.labels().get(0).text().contains("Y 64 -> 68"));
    }

    @Test
    void filtersByCurrentDimension() {
        assertTrue(planner.plan(box(), "minecraft:the_nether", new Vector3(0, 64, 0), 512).isEmpty());
    }

    private static TriggerAreaDefinitionView box() {
        var geometry = new AreaGeometrySnapshot(AreaShapeKind.BOX, new Vector3(0, 64, 0),
                new Vector3(4, 68, 4), new Vector3(2, 66, 2), OptionalDouble.empty(), List.of());
        return new TriggerAreaDefinitionView(new TriggerId("jdh:house_exit"), "Area",
                "minecraft:overworld", new TriggerRevision(1), geometry, Optional.empty(), List.of());
    }
}
