package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class InlineDialogueSubscriptionTest {
    @Test void inlineReactionDoesNotRunAsDialogueNodeOrChangeEntry() {
        var result = new EchoScriptDialogueCompiler().compile("jdh:echo_dialogue/dialogues/mother.dialogue.es", """
                class Mother : DialogueResource {
                    Node Intro() { Line("john", "Hello."); }
                    When(Dialogue.Completed("jdh:father"), id: "after_father") {
                        Dialogue.Play("jdh:mother");
                    }
                }
                """);
        assertTrue(result.successful(), result.diagnostics().toString());
        var dialogue = result.dialogue().orElseThrow();
        assertEquals("jdh:mother", dialogue.id().toString());
        assertEquals(1, dialogue.nodes().size());
        assertEquals("intro", dialogue.startNodeId().toString());
    }
}
