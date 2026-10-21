package io.github.kxyr0.echocontentdoctor.events;

import static org.junit.jupiter.api.Assertions.*;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctor;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorInput;
import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentNode;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echocontentdoctor.source.adapter.events.EventTriggerSourceAdapter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EventDoctorTest {
    @Test void detectsMissingSourceAndTargetWithSuggestionAndExactMatcherLine() {
        var trigger = trigger("Dialogue.Completed(\"jdh:father_dialgue\")", "Scene.Play(\"jdh:missing_scene\")");
        var result = validate(trigger, simple("jdh:father_dialogue", ContentResourceKind.DIALOGUE));
        var missing = result.diagnostics().stream().filter(value -> value.code().equals("ECHO_DOCTOR_EVENT_SOURCE_NOT_FOUND"))
                .findFirst().orElseThrow();
        assertEquals(List.of(new ContentResourceId("jdh:father_dialogue")), missing.suggestions());
        assertEquals(4, missing.span().start().line());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_ACTION_TARGET_NOT_FOUND")));
    }
    @Test void checksActualChoiceIdentityDuplicateSubscriptionAndProvableSceneCycle() {
        var dialogue = simple("jdh:father", ContentResourceKind.DIALOGUE).withNodes(List.of(new ContentNode("Start", true,
                false, List.of(), SourceSpans.file("father.dialogue.es", ""), List.of("tell_truth"))));
        var good = trigger("Dialogue.Choice(\"jdh:father\", \"tell_truth\")", "Scene.Play(\"jdh:scene\")");
        assertFalse(validate(good, dialogue, simple("jdh:scene", ContentResourceKind.SCENE)).diagnostics().stream()
                .anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_CHOICE_NOT_FOUND")));
        var bad = trigger("Dialogue.Choice(\"jdh:father\", \"tell_truh\")", "Scene.Play(\"jdh:scene\")");
        assertTrue(validate(bad, dialogue).diagnostics().stream().anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_CHOICE_NOT_FOUND")));
        assertTrue(validate(good, good, dialogue).diagnostics().stream().anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_SUBSCRIPTION_DUPLICATE")));
        var cycle = trigger("Scene.Completed(\"jdh:scene\")", "Scene.Play(\"jdh:scene\")");
        assertTrue(validate(cycle, simple("jdh:scene", ContentResourceKind.SCENE)).diagnostics().stream()
                .anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_CYCLE")));
    }
    @Test void worldEventRejectsPlayerActionAndBrokenSyntaxProducesDiagnostic() {
        var world = trigger("Npc.Died(\"jdh:father\")", "Scene.Play(\"jdh:scene\")");
        assertTrue(validate(world).diagnostics().stream().anyMatch(value -> value.code().equals("ECHO_DOCTOR_EVENT_CONTEXT_REQUIRED")));
        assertFalse(new EventTriggerSourceAdapter().adapt("broken.trigger.es", "class broken").problems().isEmpty());
    }
    @Test void observationAdapterCompilesDatapackPathUsingNativeNamespacedSourceIdentity() {
        var adapter = new io.github.kxyr0.echocontentdoctor.source.adapter.events.ObservationSourceAdapter(
                io.github.kxyr0.echoscene.compile.SceneSymbolCatalog.of(List.of()));
        var result = adapter.adapt("data/jdh/echo_observation/observations/house.es",
                "class House : ObservationResource { Observation Photo() { Focus(1.0); } }");
        assertTrue(result.problems().stream().noneMatch(value -> value.diagnostic().severity()
                == io.github.kxyr0.echocontentdoctor.model.ContentSeverity.ERROR), result.problems().toString());
        assertEquals("jdh:house", result.descriptors().get(0).id().value());
        assertEquals("photo", result.descriptors().get(0).nodes().get(0).id());
    }
    private ContentDescriptor trigger(String selector, String action) {
        String text = """
                class Test : TriggerResource {
                    private void Init() {
                        Id("jdh:reaction");
                        When(%s);
                    }
                    private void Then() { %s; }
                }
                """.formatted(selector, action);
        var adapted = new EventTriggerSourceAdapter().adapt("reaction.trigger.es", text);
        assertTrue(adapted.problems().isEmpty(), adapted.problems().toString());
        return adapted.descriptors().get(0);
    }
    private ContentDescriptor simple(String id, ContentResourceKind kind) {
        return ContentDescriptor.simple(new ContentResourceId(id), kind, SourceSpans.file("source.es", ""), List.of());
    }
    private io.github.kxyr0.echocontentdoctor.engine.ContentDoctorResult validate(ContentDescriptor... descriptors) {
        return new ContentDoctor().validate(ContentDoctorInput.catalogOnly(new ContentCatalogSnapshot(1, descriptors.length, List.of(descriptors))));
    }
}
