package io.github.kxyr0.echodialogue.echoscript;

import static io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueSources.duplicateNodeMethod;
import static io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueSources.validSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;

import org.junit.jupiter.api.Test;

class EchoScriptDialogueCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void validDialogueResourceCompilesToImmutableLinearDialogue() {
        DialogueCompileResult result = compiler.compile("sample/sample_greeting.dialogue.es", validSource());

        assertTrue(result.successful());
        CompiledDialogue dialogue = result.dialogue().orElseThrow();
        assertEquals("sample:greeting", dialogue.id().toString());
        assertEquals("greeting", dialogue.startNodeId().toString());
        assertEquals(3, dialogue.lines().size());
        assertEquals("Отец", dialogue.lines().get(0).speakerDisplayName());
        assertEquals("Где ты был?", dialogue.lines().get(0).text());
        assertEquals("Гулял.", dialogue.lines().get(1).text());
    }

    @Test
    void displayFallsBackToAliasWhenAbsent() {
        String source = validSource().replace("Alias(\"player\", \"sample:player\", display: \"Игрок\");",
                "Alias(\"player\", \"sample:player\");");

        DialogueCompileResult result = compiler.compile("sample/fallback.dialogue.es", source);

        assertEquals("player", result.dialogue().orElseThrow().lines().get(1).speakerDisplayName());
    }

    @Test
    void missingStartNodeDoesNotProduceCompiledDialogue() {
        String source = validSource().replace("Start(\"greeting\");", "Start(\"missing\");");

        DialogueCompileResult result = compiler.compile("sample/missing.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-007")));
    }

    @Test
    void duplicateNodeIdIsRejected() {
        String source = validSource().replace("private NodeDef Greeting()", duplicateNodeMethod());

        DialogueCompileResult result = compiler.compile("sample/duplicate.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-017")));
    }

    @Test
    void unresolvedAliasIsRejected() {
        String source = validSource().replace("Line(\"player\", \"Гулял.\");", "Line(\"ghost\", \"Гулял.\");");

        DialogueCompileResult result = compiler.compile("sample/unresolved.dialogue.es", source);

        assertFalse(result.successful());
        var diagnostic = result.diagnostics().stream()
                .filter(value -> value.code().equals("EDLG-SEM-011"))
                .findFirst()
                .orElseThrow();
        assertEquals("sample/unresolved.dialogue.es", diagnostic.subject().orElseThrow().value());
        assertTrue(diagnostic.subject().orElseThrow().line().isPresent());
        assertTrue(diagnostic.subject().orElseThrow().column().isPresent());
    }

    @Test
    void emptyDialogueNodeIsRejected() {
        String source = validSource()
                .replace("        Line(\"father\", \"Где ты был?\");\n", "")
                .replace("        Line(\"player\", \"Гулял.\");\n", "")
                .replace("        Line(\"father\", \"Понятно.\");\n", "");

        DialogueCompileResult result = compiler.compile("sample/empty.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-018")));
    }

    @Test
    void lineNamedPropertiesCompileAsExtensionCues() {
        String source = validSource().replace("Line(\"father\", \"Где ты был?\");",
                "Line(\"father\", \"Где ты был?\", face: \"angry\");");

        DialogueCompileResult result = compiler.compile("sample/face.dialogue.es", source);

        assertTrue(result.successful());
        assertTrue(result.dialogue().orElseThrow().lines().get(0).cues()
                .contains(new CompiledDialogueLine.Cue("property.face", "angry")));
    }

    @Test
    void invalidDialogueIdIsDiagnosticNotCompiledOutput() {
        String source = validSource().replace("Dialogue(\"sample:greeting\")", "Dialogue(\"SampleGreeting\")");

        DialogueCompileResult result = compiler.compile("sample/invalid_id.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-013")));
    }

    @Test
    void equivalentSourceCompilesDeterministically() {
        DialogueCompileResult first = compiler.compile("sample/a.dialogue.es", validSource());
        DialogueCompileResult second = compiler.compile("sample/b.dialogue.es", validSource());

        assertEquals(first.dialogue(), second.dialogue());
    }

    @Test
    void malformedSourceNeverProducesCompiledOutput() {
        DialogueCompileResult result = compiler.compile(
                "sample/broken.dialogue.es",
                "class Broken : DialogueResource {");

        assertFalse(result.successful());
        assertTrue(result.dialogue().isEmpty());
        var diagnostic = result.diagnostics().stream()
                .filter(value -> value.category() == DiagnosticCategory.SYNTAX_ADAPTATION)
                .findFirst()
                .orElseThrow();
        assertEquals("sample/broken.dialogue.es", diagnostic.subject().orElseThrow().value());
        assertTrue(diagnostic.subject().orElseThrow().line().isPresent());
        assertTrue(diagnostic.subject().orElseThrow().column().isPresent());
    }


}

