package io.github.kxyr0.echodebug.forge.integration.seating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodebug.core.path.TriggerIdPlanner;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.core.binding.TriggerDefinitionBinder;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeatTriggerSourceFactoryTest {
    @Test
    void generatedSourceBindsToExactSeatAndOncePerPlayerStory() {
        var id = new TriggerIdPlanner().plan("jdh", "trigger/john_seat").value().orElseThrow();
        var seat = new SeatSelectionResolver.SeatReference("jdh:table", "jdh:table/john");
        String source = SeatTriggerSourceFactory.render(id, seat, "jdh:chapter_1/breakfast");
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("seat.trigger.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        var bound = new TriggerDefinitionBinder().bind(parsed.syntaxTree().orElseThrow(), "jdh",
                new TriggerRevision(1));
        assertTrue(bound.successful(), () -> bound.diagnostics().toString());
        var subscription = bound.definition().orElseThrow().storySubscription().orElseThrow();
        assertEquals(Optional.of("jdh:table"), subscription.selector().zoneId());
        assertEquals(Optional.of("jdh:table/john"), subscription.selector().slotId());
        assertEquals("jdh:chapter_1/breakfast", subscription.storyId());
        assertTrue(subscription.fireOnlyOnce());
        assertTrue(subscription.selector().sameSeatingEvent("echo_seating:events", "SEATED",
                "jdh:table", Optional.of("jdh:table/john")));
    }

    @Test
    void rejectsInvalidStoryIdBeforeItCanBecomeSourceCode() {
        var id = new TriggerIdPlanner().plan("jdh", "seat").value().orElseThrow();
        var seat = new SeatSelectionResolver.SeatReference("jdh:table", "jdh:table/john");
        assertThrows(IllegalArgumentException.class, () -> SeatTriggerSourceFactory.render(id, seat, ""));
        assertThrows(IllegalArgumentException.class, () -> SeatTriggerSourceFactory.render(id, seat, "jdh:\"bad"));
    }
}
