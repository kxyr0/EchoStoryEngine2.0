package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TriggerSeatingSubscriptionBinderTest {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    @Test
    void bindsGenericSeatedInSubscription() {
        TriggerCompileResult result = bind("""
                class BreakfastSeatTrigger : TriggerResource
                {
                    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
                    private StoryId BreakfastStory = "jdh:chapter_1/breakfast";
                    private void Init()
                    {
                        Id("jdh:breakfast_seat_trigger");
                        Subscribe(Player.SeatedIn(BreakfastTable));
                        StartStory(BreakfastStory);
                        FireOnlyOncePerPlayer();
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var subscription = result.definition().orElseThrow().storySubscription().orElseThrow();
        assertEquals("echo_seating:events", subscription.selector().providerId());
        assertEquals("SEATED", subscription.selector().eventKind());
        assertEquals(Optional.of("jdh:home/breakfast_table"), subscription.selector().zoneId());
        assertTrue(subscription.selector().slotId().isEmpty());
        assertEquals(TriggerSubscriptionTargetKind.STORY, subscription.targetKind());
        assertEquals("jdh:chapter_1/breakfast", subscription.storyId());
        assertTrue(subscription.fireOnlyOnce());
    }

    @Test
    void bindsSpecificSeatedAtSubscription() {
        TriggerCompileResult result = bind("""
                class MothersSeatTrigger : TriggerResource
                {
                    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
                    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";
                    private void Init()
                    {
                        Id("jdh:mothers_seat_trigger");
                        Subscribe(Player.SeatedAt(BreakfastTable, MothersSeat));
                        StartStory("jdh:chapter_1/mothers_seat");
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var selector = result.definition().orElseThrow().storySubscription().orElseThrow().selector();
        assertEquals("echo_seating:events|SEATED|jdh:home/breakfast_table"
                + "|slot=jdh:home/breakfast_table/seat_3", selector.exactKey());
        assertTrue(selector.sameSeatingEvent("echo_seating:events", "SEATED",
                "jdh:home/breakfast_table", Optional.of("jdh:home/breakfast_table/seat_3")));
    }

    @Test
    void bindsSpecificSeatedAtSubscriptionWithoutZone() {
        TriggerCompileResult result = bind("""
                class MothersSeatTrigger : TriggerResource
                {
                    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";
                    private void Init()
                    {
                        Id("jdh:mothers_seat_trigger");
                        Subscribe(Player.SeatedAt(MothersSeat));
                        StartStory("jdh:chapter_1/mothers_seat");
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var selector = result.definition().orElseThrow().storySubscription().orElseThrow().selector();
        assertTrue(selector.zoneId().isEmpty());
        assertEquals(Optional.of("jdh:home/breakfast_table/seat_3"), selector.slotId());
        assertEquals("echo_seating:events|SEATED|slot=jdh:home/breakfast_table/seat_3", selector.exactKey());
        assertTrue(selector.sameSeatingEvent("echo_seating:events", "SEATED",
                "jdh:any_zone", Optional.of("jdh:home/breakfast_table/seat_3")));
    }

    @Test
    void bindsSpecificSeatedAtDialogueSubscription() {
        TriggerCompileResult result = bind("""
                class MothersSeatDialogueTrigger : TriggerResource
                {
                    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";
                    private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";
                    private DialogueId MothersSeatDialogue = "jdh:dialogue/wrong_mothers_seat";
                    private void Init()
                    {
                        Id("jdh:mothers_seat_dialogue_trigger");
                        Subscribe(Player.SeatedAt(BreakfastTable, MothersSeat));
                        StartDialogue(MothersSeatDialogue);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var subscription = result.definition().orElseThrow().storySubscription().orElseThrow();
        assertEquals(TriggerSubscriptionTargetKind.DIALOGUE, subscription.targetKind());
        assertEquals("jdh:dialogue/wrong_mothers_seat", subscription.dialogueId());
        assertEquals("echo_seating:events|SEATED|jdh:home/breakfast_table"
                + "|slot=jdh:home/breakfast_table/seat_3", subscription.selector().exactKey());
    }

    @Test
    void rejectsEmptySeatedAt() {
        TriggerCompileResult result = bind("""
                class BadSeatTrigger : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:bad_seat_trigger");
                        Subscribe(Player.SeatedAt());
                        StartStory("jdh:chapter_1/bad");
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == TriggerDiagnosticCode.TRIGGER_SEATING_SELECTOR_INVALID));
    }

    private TriggerCompileResult bind(String source) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("test.trigger.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        return binder.bind(parsed.syntaxTree().orElseThrow(), "test", new TriggerRevision(1));
    }
}
