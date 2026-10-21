package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import org.junit.jupiter.api.Test;

class EchoScriptDialogueVisualCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void aliasWithPortraitVisualCompiles() {
        String source = validSource().replace("Alias(\"father\", \"sample:father\", display: \"Отец\");",
                "Alias(\"father\", \"sample:father\", display: \"Отец\", "
                        + "visual: Portrait(\"sample:father_portrait\"));");

        DialogueCompileResult result = compiler.compile("sample/portrait.dialogue.es", source);

        assertTrue(result.successful());
        var visual = result.dialogue().orElseThrow().lines().get(0).speakerVisual().orElseThrow();
        assertEquals(SpeakerVisualKind.PORTRAIT, visual.kind());
        assertEquals("sample:father_portrait", visual.id().toString());
    }

    @Test
    void aliasWithSkinHeadVisualCompiles() {
        String source = validSource().replace("Alias(\"father\", \"sample:father\", display: \"Отец\");",
                "Alias(\"father\", \"sample:father\", display: \"Отец\", "
                        + "visual: SkinHead(\"sample:father\"));");

        DialogueCompileResult result = compiler.compile("sample/skin.dialogue.es", source);

        assertTrue(result.successful());
        assertEquals(SpeakerVisualKind.SKIN_HEAD,
                result.dialogue().orElseThrow().lines().get(0).speakerVisual().orElseThrow().kind());
    }

    @Test
    void aliasWithNameColorCompiles() {
        String source = validSource().replace("Alias(\"player\", \"sample:player\", display:",
                "Alias(\"player\", \"sample:player\", nameColor: nameColor(\"#57C7FFFF\"), display:");

        DialogueCompileResult result = compiler.compile("sample/name_color.dialogue.es", source);

        assertTrue(result.successful());
        assertEquals("#57C7FF",
                result.dialogue().orElseThrow().lines().get(1).speakerNameColor().orElseThrow().hex());
    }

    @Test
    void invalidVisualDefinitionsDoNotCompile() {
        assertVisualDiagnostic("visual: Icon(\"sample:father\")", "EDLG-SEM-031");
        assertVisualDiagnostic("visual: Portrait(\"sample:father.png\")", "EDLG-SEM-032");
        assertVisualDiagnostic("visual: \"sample:father\"", "EDLG-SEM-029");
    }

    @Test
    void invalidNameColorDefinitionsDoNotCompile() {
        assertNameColorDiagnostic("nameColor: \"#57C7FF\"", "EDLG-SEM-047");
        assertNameColorDiagnostic("nameColor: SkinHead(\"sample:father\")", "EDLG-SEM-048");
        assertNameColorDiagnostic("nameColor: nameColor(\"bad\")", "EDLG-SEM-049");
    }

    @Test
    void perLineVisualOverrideIsRejected() {
        String source = validSource().replace("Line(\"father\", \"Где ты был?\");",
                "Line(\"father\", \"Где ты был?\", visual: Portrait(\"sample:father\"));");

        DialogueCompileResult result = compiler.compile("sample/line_visual.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.category() == DiagnosticCategory.UNSUPPORTED_DIALOGUE_RESOURCE_FEATURE
                        && diagnostic.message().contains("visual")));
    }

    private void assertVisualDiagnostic(String visualArgument, String code) {
        String source = validSource().replace("Alias(\"father\", \"sample:father\", display: \"Отец\");",
                "Alias(\"father\", \"sample:father\", display: \"Отец\", " + visualArgument + ");");

        DialogueCompileResult result = compiler.compile("sample/invalid_visual.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }

    private void assertNameColorDiagnostic(String colorArgument, String code) {
        String source = validSource().replace("Alias(\"father\", \"sample:father\", display:",
                "Alias(\"father\", \"sample:father\", " + colorArgument + ", display:");

        DialogueCompileResult result = compiler.compile("sample/invalid_name_color.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }

    private static String validSource() {
        return """
                namespace EchoDialogue.Sample;

                class SampleGreeting : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Greeting() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:greeting");
                    }

                    private void Init()
                    {
                        Start("greeting");
                        Alias("father", "sample:father", display: "Отец");
                        Alias("player", "sample:player", display: "Игрок");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Greeting()
                    {
                        var node = new Node("greeting");

                        Line("father", "Где ты был?");
                        Line("player", "Гулял.");
                        Line("father", "Понятно.");

                        return node;
                    }
                }
                """;
    }
}
