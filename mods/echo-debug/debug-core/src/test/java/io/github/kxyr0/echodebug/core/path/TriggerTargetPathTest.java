package io.github.kxyr0.echodebug.core.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TriggerTargetPathTest {
    private final TriggerIdPlanner planner = new TriggerIdPlanner();

    @Test
    void plansNestedPathUnderStoryTriggers() {
        TriggerIdPlan id = planner.plan("jdh", "chapter_1/house_exit").value().orElseThrow();
        TriggerTargetPath path = TriggerTargetPath.plan(Path.of("game"), id).value().orElseThrow();

        assertTrue(path.finalPath().endsWith(Path.of("jdh", "chapter_1", "house_exit.trigger.es")));
    }

    @Test
    void rejectsUppercaseAndTraversal() {
        assertFalse(planner.plan("jdh", "Chapter_1/house_exit").successful());
        assertFalse(planner.plan("jdh", "../house_exit").successful());
    }

    @Test
    void derivesClassNameFromLeafPath() {
        TriggerIdPlan id = planner.plan("jdh", "chapter_1/house_exit").value().orElseThrow();

        assertEquals("HouseExit", id.className());
    }
}
