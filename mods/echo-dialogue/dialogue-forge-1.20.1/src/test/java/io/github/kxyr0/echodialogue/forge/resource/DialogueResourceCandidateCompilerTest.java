package io.github.kxyr0.echodialogue.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueCompiler;
import io.github.kxyr0.echodialogue.forge.catalog.CatalogInstallResult;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialogueResourceCandidateCompilerTest {
    @Test
    void compilesDiscoveredDialogueResourceText() {
        DialogueResourceCandidateCompiler compiler =
                new DialogueResourceCandidateCompiler(new EchoScriptDialogueCompiler());

        DialogueCatalogCandidate candidate = compiler.compile(new DialogueResourceReadResult(
                List.of(new DialogueResourceText("sample:sample_greeting.dialogue.es", source())), List.of()));

        assertTrue(candidate.diagnostics().stream().noneMatch(diagnostic -> diagnostic.isError()));
        assertEquals(1, candidate.dialogues().size());
        assertEquals(DialogueId.parse("sample:greeting"),
                candidate.sourceDialogueIds().get("sample:sample_greeting.dialogue.es"));
        assertEquals("Где ты был?", candidate.dialogues().get(0).lines().get(0).text());
    }

    @Test
    void malformedResourceDoesNotProduceCompiledDialogue() {
        DialogueResourceCandidateCompiler compiler =
                new DialogueResourceCandidateCompiler(new EchoScriptDialogueCompiler());

        DialogueCatalogCandidate candidate = compiler.compile(new DialogueResourceReadResult(
                List.of(new DialogueResourceText("sample:broken.dialogue.es", "class Broken {")), List.of()));

        assertFalse(candidate.diagnostics().isEmpty());
        assertTrue(candidate.dialogues().isEmpty());
    }

    @Test
    void semanticInvalidCandidateRetainsKnownGoodCatalogAndFixedCandidateApplies() {
        DialogueResourceCandidateCompiler compiler =
                new DialogueResourceCandidateCompiler(new EchoScriptDialogueCompiler());
        DialogueCatalogState state = new DialogueCatalogState();
        CatalogInstallResult initial = state.install(compiler.compile(read(source())));
        int initialRevision = state.current().revision();

        String invalid = source().replace("Line(\"father\", \"Где ты был?\");", "Line(\"ghost\", \"Где ты был?\");");
        CatalogInstallResult rejected = state.install(compiler.compile(read(invalid)));

        assertTrue(initial.installed());
        assertFalse(rejected.installed());
        assertEquals(initialRevision, state.current().revision());
        assertTrue(state.current().find(DialogueId.parse("sample:greeting")).isPresent());
        assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("EDLG-SEM-011")
                        && diagnostic.subject().isPresent()
                        && diagnostic.subject().orElseThrow().line().isPresent()));

        CatalogInstallResult fixed = state.install(compiler.compile(read(source())));

        assertTrue(fixed.installed());
        assertEquals(initialRevision + 1, state.current().revision());
    }

    @Test
    void collidingInferredFileIdsRejectReloadWithBothSources() {
        DialogueResourceCandidateCompiler compiler =
                new DialogueResourceCandidateCompiler(new EchoScriptDialogueCompiler());
        String highLevel = "class X : DialogueResource { Node Intro() { Line(\"john\", \"A\"); } }";
        DialogueResourceReadResult read = new DialogueResourceReadResult(List.of(
                new DialogueResourceText(
                        "jdh:echo_dialogue/dialogues/chapter_1/a/remember.dialogue.es", highLevel),
                new DialogueResourceText(
                        "jdh:echo_dialogue/dialogues/chapter_1/b/remember.dialogue.es", highLevel)), List.of());

        CatalogInstallResult result = new DialogueCatalogState().install(compiler.compile(read));

        assertFalse(result.installed());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("ECHO_SCRIPT_RESOURCE_ID_COLLISION")
                        && diagnostic.message().contains("chapter_1/a")
                        && diagnostic.message().contains("chapter_1/b")));
    }

    private static DialogueResourceReadResult read(String source) {
        return new DialogueResourceReadResult(
                List.of(new DialogueResourceText("sample:sample_greeting.dialogue.es", source)), List.of());
    }

    private static String source() {
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
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Greeting()
                    {
                        var node = new Node("greeting");
                        Line("father", "Где ты был?");
                        return node;
                    }
                }
                """;
    }
}
