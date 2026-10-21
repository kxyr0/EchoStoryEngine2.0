package io.github.kxyr0.echotrigger.core.reload;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriggerReloadPlannerTest {
    @Test
    void unchangedSourceDoesNotCallParserOrBinderAgain() {
        TriggerReloadPlanner planner = new TriggerReloadPlanner();
        Path file = Path.of("jdh/echo_trigger/triggers/a.trigger.es");

        var first = planner.plan(Map.of(file, "class A : TriggerResource {}"));
        var second = planner.plan(Map.of(file, "class A : TriggerResource {}"));

        assertEquals(1, first.changedFiles().size());
        assertEquals(1, first.parserCalls());
        assertEquals(0, second.changedFiles().size());
        assertEquals(0, second.binderCalls());
    }
}
