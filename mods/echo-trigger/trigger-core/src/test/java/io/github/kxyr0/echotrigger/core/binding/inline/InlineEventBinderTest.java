package io.github.kxyr0.echotrigger.core.binding.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

final class InlineEventBinderTest {
    private static final String FIRST = """
            When(Dialogue.Completed("jdh:father"), id: "after_father") {
                Observation.Activate("jdh:house");
                Scene.Play("jdh:father_leaves");
            }
            """;
    private static final String SECOND = """
            When(Dialogue.Choice("jdh:father", "tell_truth"), id: "truth", once: true) {
                Dialogue.Play("jdh:mother");
            }
            """;

    @Test void supportsAllThreeProfilesAndStableIndependentHandlerIdentity() {
        for (String profile : java.util.List.of("StoryResource", "DialogueResource", "TriggerResource")) {
            var result = bind(profile, FIRST + SECOND);
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
            assertEquals(2, result.definitions().size());
            assertEquals(2, result.definitions().get(0).actions().size());
            assertTrue(result.definitions().get(1).subscription().once());
            var reordered = bind(profile, SECOND + FIRST);
            assertEquals(result.definitions().get(0).subscription().id(),
                    reordered.definitions().get(1).subscription().id());
        }
    }

    @Test void rejectsDuplicatesUnknownHeaderEmptyBodyAndUnknownActionsAtomically() {
        for (String body : java.util.List.of(FIRST + FIRST, FIRST.replace("id:", "unknown:"),
                FIRST.replace("after_father", "UPPER"), FIRST.replace("Scene.Play", "Scene.Unknown"),
                "When(Dialogue.Completed(\"jdh:father\"), id: \"empty\") {}",
                SECOND.replace("once: true", "once: \"yes\""))) {
            var result = bind("StoryResource", body);
            assertFalse(result.diagnostics().isEmpty(), body);
            assertTrue(result.definitions().isEmpty());
        }
    }

    @Test void sourceCoordinatesAndProfileNamespacesRemainDistinct() {
        var invalid = bind("DialogueResource", FIRST.replace("Scene.Play", "Scene.Unknown"));
        assertEquals(5, invalid.diagnostics().get(0).source().orElseThrow().line());
        assertFalse(bind("StoryResource", FIRST).definitions().get(0).subscription().id().equals(
                bind("DialogueResource", FIRST).definitions().get(0).subscription().id()));
    }

    private InlineEventResult bind(String profile, String body) {
        String text = "class Example : " + profile + " {\nId(\"jdh:owner\");\n" + body + "}";
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("jdh:echo_story/stories/owner.es"), text));
        assertTrue(parsed.successful(), parsed.diagnostics().toString());
        return new InlineEventBinder().bind(parsed.syntaxTree().orElseThrow(), "revision-1");
    }
}
