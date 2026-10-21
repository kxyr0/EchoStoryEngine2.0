package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

final class TriggerDomainEventsBinderTest {
    @Test void realParserBindsCompletionChoiceAndOrderedActions() {
        var completed = bind("Dialogue.Completed(\"jdh:father_dialogue\")");
        assertTrue(completed.successful(), completed.diagnostics().toString());
        var definition = completed.definition().orElseThrow().eventSubscription().orElseThrow();
        assertEquals(EventKind.DIALOGUE_COMPLETED, definition.subscription().filter().kind());
        assertEquals("jdh:father_finished#When", definition.subscription().id().value());
        assertEquals(2, definition.actions().size());
        var catalog = new io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog(
                java.util.List.of(completed.definition().orElseThrow()));
        assertTrue(catalog.runtimeView().definitions().isEmpty());
        assertTrue(definition.matcherSource().line() > 0);
        var choice = bind("Dialogue.Choice(\"jdh:father_argument\", \"tell_truth\")");
        assertTrue(choice.successful(), choice.diagnostics().toString());
        assertEquals("tell_truth", choice.definition().orElseThrow().eventSubscription().orElseThrow()
                .subscription().filter().detail().orElseThrow().value());
        var marker = bind("Animation.Marker(\"jdh:father\", \"jdh:take_letter\", \"jdh:grab\")");
        assertTrue(marker.successful(), marker.diagnostics().toString());
        assertEquals("jdh:father", marker.definition().orElseThrow().eventSubscription().orElseThrow()
                .subscription().filter().subject().orElseThrow().value());
    }
    @Test void unsupportedAndMalformedPredicatesFailBinding() {
        assertFalse(bind("Dialogue.Wrong(\"jdh:father\")").successful());
        assertFalse(bind("Dialogue.Choice(\"jdh:father\")").successful());
        assertFalse(bind("Dialogue.Completed(\"jdh:father\", \"extra\")").successful());
    }
    private io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult bind(String predicate) {
        String source = """
                class FatherFinished : TriggerResource {
                    private void Init() {
                        Id("jdh:father_finished");
                        When(%s);
                    }
                    private void Then() {
                        Observation.Activate("jdh:house_after_father");
                        Scene.Play("jdh:father_leaves");
                    }
                }
                """.formatted(predicate);
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("father.trigger.es"), source));
        assertTrue(parsed.successful(), parsed.diagnostics().toString());
        return new TriggerDefinitionBinder().bind(parsed.syntaxTree().orElseThrow(),
                "revision-1", new TriggerRevision(1));
    }
}
