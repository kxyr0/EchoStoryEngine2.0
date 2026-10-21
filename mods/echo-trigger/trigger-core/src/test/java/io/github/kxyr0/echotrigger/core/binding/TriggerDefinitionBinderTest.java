package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerDefinitionBinderTest {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    @Test
    void bindsTriggerResourceWithOrderedPlayerEnterSequence() {
        TriggerCompileResult result = bind(leaveHouse());

        assertTrue(result.successful());
        assertEquals("jdh:leave_house", result.definition().orElseThrow().id().value());
        assertEquals(TriggerScope.PLAYER, result.definition().orElseThrow().scope());
        assertEquals(TriggerMode.ONCE, result.definition().orElseThrow().mode());
        assertEquals(3, result.definition().orElseThrow().sequence().size());
    }

    @Test
    void rejectsEdgeEventInsideIfBecauseIfIsSynchronous() {
        TriggerCompileResult result = bind("""
                class Bad : TriggerResource
                {
                    private Area Exit = Area("minecraft:overworld", 1, 2, 3);
                    private void Init() { Id("jdh:bad"); Start(Run); }
                    private void Run()
                    {
                        if (PlayerEnter(Exit)) {
                            Complete();
                        }
                    }
                }
                """);

        assertFalse(result.successful());
    }

    @Test
    void bindsT2PredicateAndEventExpressions() {
        TriggerCompileResult result = bind("""
                class T2 : TriggerResource
                {
                    private Area Exit = Area("minecraft:overworld", 1, 2, 3);
                    private void Init() { Id("jdh:t2"); Start(Run); }
                    private void Run()
                    {
                        Wait(HasItem("minecraft:diamond", count: 3));
                        Wait(ItemAdded("#minecraft:wool", count: 2));
                        Wait(BlockInteracted("#minecraft:doors", area: Exit, hand: MAIN_HAND));
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals("HasItem", result.definition().orElseThrow().sequence().get(0)
                .expression().condition().expressionName());
        assertEquals("3", result.definition().orElseThrow().sequence().get(0)
                .expression().condition().arguments().get("count"));
    }

    @Test
    void rejectsInvalidItemCount() {
        TriggerCompileResult result = bind("""
                class BadCount : TriggerResource
                {
                    private void Init() { Id("jdh:bad_count"); Start(Run); }
                    private void Run() { Wait(HasItem("minecraft:diamond", count: 0)); }
                }
                """);

        assertFalse(result.successful());
        assertEquals(TriggerDiagnosticCode.TRIGGER_ITEM_COUNT_INVALID, result.diagnostics().get(0).code());
    }

    private TriggerCompileResult bind(String source) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("test.trigger.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        return binder.bind(parsed.syntaxTree().orElseThrow(), "test", new TriggerRevision(1));
    }

    private static String leaveHouse() {
        return """
                class LeaveHouse : TriggerResource
                {
                    private Area Hall = Area("minecraft:overworld", 180, 4, 192, 4, 3, 4);
                    private Area Stairs = Area("minecraft:overworld", 190, 4, 196, 3, 4, 3);
                    private Area Exit = Area("minecraft:overworld", 200, 4, 200, 3, 3, 3);
                    private void Init()
                    {
                        Id("jdh:leave_house");
                        Scope(PLAYER);
                        Mode(ONCE);
                        Start(LeaveHouseSequence);
                    }
                    private void LeaveHouseSequence()
                    {
                        Wait(PlayerEnter(Hall));
                        Wait(PlayerEnter(Stairs));
                        Wait(PlayerEnter(Exit));
                        Complete();
                    }
                }
                """;
    }
}
