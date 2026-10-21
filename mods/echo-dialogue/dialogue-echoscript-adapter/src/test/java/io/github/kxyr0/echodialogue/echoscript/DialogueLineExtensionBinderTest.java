package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinding;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineExtensionCue;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialogueLineExtensionBinderTest {
    @Test
    void realCompactDslPassesNestedVoiceExpressionToRegisteredBinder() {
        DialogueLineArgumentBinder voice = new DialogueLineArgumentBinder() {
            @Override public String argumentName() { return "voice"; }
            @Override public DialogueLineArgumentBinding bind(ExpressionSyntax value) {
                boolean valid = value instanceof InvocationExpressionSyntax invocation
                        && invocation.target() instanceof NameExpressionSyntax name && name.name().equals("Voice");
                return valid ? DialogueLineArgumentBinding.accepted(List.of(
                        new DialogueLineExtensionCue("voice.test", "accepted")))
                        : DialogueLineArgumentBinding.rejected("VOICE_TEST_REJECTED", "Voice call expected.");
            }
        };

        DialogueCompileResult result = new EchoScriptDialogueCompiler(List.of(voice))
                .compile("sample/voice.dialogue.es", source());

        assertTrue(result.successful(), result.diagnostics().toString());
        assertEquals("accepted", result.dialogue().orElseThrow().lines().get(0).cues().stream()
                .filter(cue -> cue.name().equals("voice.test")).findFirst().orElseThrow().value());
    }

    @Test
    void nestedVoiceIsNeverSilentlyIgnoredWithoutProvider() {
        DialogueCompileResult result = new EchoScriptDialogueCompiler().compile("sample/voice.dialogue.es", source());

        assertTrue(result.dialogue().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().contains("voice")));
    }

    private String source() {
        return """
                class VoiceDialogue : DialogueResource {
                    private void Init() {
                        Id("sample:voice");
                        Start(Greeting);
                        Alias("john", "sample:john");
                    }
                    private void Greeting() {
                        Line("john", "Text.", voice: Voice("john_001", volume: 0.8));
                    }
                }
                """;
    }
}
