package io.github.kxyr0.echoquests.echoscript;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import io.github.kxyr0.echoquests.api.model.QuestStateKind;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestScriptCompilerTest {
    @Test
    void compilesEs2StateMachineAndInfersNestedResourceId() {
        String source = """
                class FindNotebookQuest : QuestResource
                {
                    Title("Потерянный блокнот");
                    Initial("find");
                    State("find")
                    {
                        Objective("Найдите блокнот");
                        On("notebook_found", "return");
                    }
                    State("return")
                    {
                        Objective("Верните блокнот до 18:00");
                        On("notebook_returned", "done");
                        At(
                            "18:00",
                            when: All(
                                Player.InArea("minecraft:overworld", x: 42, y: 64, z: 100, radius: 8),
                                Player.HasItem("minecraft:writable_book", count: 1)
                            ),
                            then: "done",
                            otherwise: "failed"
                        );
                    }
                    State("done")
                    {
                        Complete();
                    }
                    State("failed")
                    {
                        Fail();
                    }
                }
                """;
        var result = new QuestScriptCompiler().compile(List.of(new QuestScriptSource(
                "data/jdh/echo_quests/quests/chapter_1/find_notebook.quest.es", source)));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var definition = result.catalog().orElseThrow().find(QuestId.parse("jdh:chapter_1/find_notebook"))
                .orElseThrow();
        assertEquals(new QuestStateId("find"), definition.initialState());
        assertEquals(new QuestStateId("return"), definition.states().get(new QuestStateId("find"))
                .transitions().get(new QuestEventId("notebook_found")));
        var deadline = definition.states().get(new QuestStateId("return")).timedTransitions().get(0);
        assertEquals("18:00", deadline.time());
        assertEquals("all", deadline.condition().type());
        assertEquals("echo_quests:player_in_area", deadline.condition().children().get(0).type());
        assertEquals("minecraft:writable_book", deadline.condition().children().get(1).arguments().get("item"));
        assertEquals(new QuestStateId("failed"), deadline.otherwiseState());
        assertEquals(QuestStateKind.COMPLETED, definition.states().get(new QuestStateId("done")).kind());
    }

    @Test
    void rejectsMissingTransitionTarget() {
        String source = """
                class BrokenQuest : QuestResource
                {
                    Title("Broken");
                    Initial("start");
                    State("start") { Objective("Start"); On("go", "missing"); }
                }
                """;
        var result = new QuestScriptCompiler().compile(List.of(new QuestScriptSource(
                "jdh:echo_quests/quests/broken.quest.es", source)));
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("missing state")));
    }
}
