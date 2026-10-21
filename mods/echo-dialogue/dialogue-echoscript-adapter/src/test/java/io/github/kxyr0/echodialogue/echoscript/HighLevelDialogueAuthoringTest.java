package io.github.kxyr0.echodialogue.echoscript;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.echoscript.binding.highlevel.NodeNameNormalizer;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighLevelDialogueAuthoringTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void acceptanceSourceLowersAllInferredInfrastructure() {
        DialogueCompileResult result = compile("university_remember", """
                class UniversityRememberDialogue : DialogueResource
                {
                    Node FirstTimeUniversity()
                    {
                        Line("john", "Кажется пора идти на пары...", thought: true);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        CompiledDialogue dialogue = result.dialogue().orElseThrow();
        assertEquals("jdh:university_remember", dialogue.id().toString());
        assertEquals("first_time_university", dialogue.startNodeId().toString());
        assertEquals("jdh:john", dialogue.lines().get(0).speakerId().toString());
        assertTrue(dialogue.startNode().completion().isPresent());
        assertTrue(dialogue.sourceInfo().orElseThrow().inferredResourceId());
        assertTrue(dialogue.sourceInfo().orElseThrow().languageNamespace().isEmpty());
        assertEquals("FirstTimeUniversity", dialogue.sourceInfo().orElseThrow().nodeSources()
                .get(new DialogueNodeId("first_time_university")).symbol());

        DialogueSessionEngine runtime = new DialogueSessionEngine();
        DialogueSessionId sessionId = new DialogueSessionId("es2-acceptance");
        var started = runtime.start(dialogue, sessionId, new AudienceId("player"));
        var completed = runtime.apply(dialogue, started.state(), DialogueInput.advance(sessionId, 0));
        assertEquals(DialogueSessionPhase.COMPLETED, completed.state().phase());
    }

    @Test
    void languageNamespaceAndClassRenameDoNotChangeGameplayIdentity() {
        String body = "{ Node Intro() { Line(\"john\", \"A\"); } }";
        DialogueCompileResult plain = compile("same", "class First : DialogueResource " + body);
        DialogueCompileResult namespaced = compile("same",
                "namespace Anything.Else; class Renamed : DialogueResource " + body);

        assertTrue(plain.successful());
        assertTrue(namespaced.successful());
        assertEquals(plain.dialogue().orElseThrow().id(), namespaced.dialogue().orElseThrow().id());
        assertEquals(plain.dialogue().orElseThrow().startNode(), namespaced.dialogue().orElseThrow().startNode());
    }

    @Test
    void fileRenameChangesInferredIdAndExplicitIdPreservesIt() {
        String inferred = "class X : DialogueResource { Node Intro() { Line(\"john\", \"A\"); } }";
        String explicit = "class X : DialogueResource { Id(\"jdh:old_name\"); "
                + "Node Intro() { Line(\"john\", \"A\"); } }";

        assertEquals("jdh:new_name", compile("new_name", inferred).dialogue().orElseThrow().id().toString());
        assertEquals("jdh:old_name", compile("new_name", explicit).dialogue().orElseThrow().id().toString());
    }

    @Test
    void multiNodeDialogueUsesExplicitEntryAndSymbolTransitionIndependentOfOrder() {
        String first = """
                class X : DialogueResource {
                    start Node A() { Line("john", "A"); GoTo(B); }
                    Node B() { Line("john", "B"); }
                }
                """;
        String reordered = """
                class X : DialogueResource {
                    Node B() { Line("john", "B"); }
                    start Node A() { Line("john", "A"); GoTo(B); }
                }
                """;

        CompiledDialogue a = compile("multi", first).dialogue().orElseThrow();
        CompiledDialogue b = compile("multi", reordered).dialogue().orElseThrow();
        assertEquals("a", a.startNodeId().toString());
        assertEquals("b", a.startNode().transition().orElseThrow().targetNodeId().orElseThrow().toString());
        assertEquals(a.startNodeId(), b.startNodeId());
    }

    @Test
    void entryNodeAndSymbolErrorsAreTyped() {
        assertCode("class X : DialogueResource { Node A() {} Node B() {} }",
                "ECHO_SCRIPT_DIALOGUE_ENTRY_MISSING");
        assertCode("class X : DialogueResource { start Node A() {} start Node B() {} }",
                "ECHO_SCRIPT_DIALOGUE_ENTRY_AMBIGUOUS");
        assertCode("class X : DialogueResource { Node A() { GoTo(Missing); } }",
                "ECHO_SCRIPT_NODE_NOT_FOUND");
    }

    @Test
    void normalizedAndFormerIdsAreDeterministic() {
        assertEquals("first_time_university", NodeNameNormalizer.normalize("FirstTimeUniversity"));
        assertEquals("room_2_entrance", NodeNameNormalizer.normalize("Room2Entrance"));
        assertEquals("http_request", NodeNameNormalizer.normalize("HTTPRequest"));
        assertCode("class X : DialogueResource { start Node FooBar() {} Node Foo_Bar() {} }",
                "ECHO_SCRIPT_NODE_ID_COLLISION");

        CompiledDialogue dialogue = compile("rename", """
                class X : DialogueResource {
                    Formerly("first_meeting", UniversityEntrance);
                    Formerly("old_entrance", UniversityEntrance);
                    Node UniversityEntrance() {}
                }
                """).dialogue().orElseThrow();
        assertEquals(Map.of(
                new DialogueNodeId("first_meeting"), new DialogueNodeId("university_entrance"),
                new DialogueNodeId("old_entrance"), new DialogueNodeId("university_entrance")),
                dialogue.formerNodeIds());
    }

    @Test
    void speakerOverrideIsLocalAndLeavesUnspecifiedPresentationDynamic() {
        CompiledDialogue dialogue = compile("unknown_girl", """
                class X : DialogueResource {
                    Speaker("sasha", display: "Незнакомка");
                    Node Intro() { Line("sasha", "Ты меня не узнаёшь?"); }
                }
                """).dialogue().orElseThrow();

        assertEquals("jdh:sasha", dialogue.lines().get(0).speakerId().toString());
        assertEquals("Незнакомка", dialogue.lines().get(0).speakerDisplayName());
        assertTrue(dialogue.lines().get(0).cues().stream()
                .anyMatch(cue -> cue.name().equals("speakerOverride.display")));
        assertTrue(dialogue.lines().get(0).speakerVisual().isEmpty());
    }

    @Test
    void emptyAndMixedResourcesAreRejectedButLegacyStillCompiles() {
        assertCode("class Empty : DialogueResource {}", "ECHO_SCRIPT_DIALOGUE_NODE_MISSING");
        assertCode("""
                class X : DialogueResource {
                    private NodeDef[] nodes = [ Legacy() ];
                    Node A() {}
                    private NodeDef Legacy() { var node = new Node("legacy"); return node; }
                }
                """, "ECHO_SCRIPT_DIALOGUE_DEFINITION_CONFLICT");

        DialogueCompileResult legacy = compiler.compile("sample:legacy.dialogue.es", """
                class Legacy : DialogueResource {
                    public override DialogueResourceDef Build() { return Dialogue("sample:legacy"); }
                    private void Init() { Start("intro"); Alias("john", "sample:john"); }
                    private NodeDef Intro() { var node = new Node("intro"); Line("john", "A"); return node; }
                }
                """);
        assertTrue(legacy.successful(), () -> legacy.diagnostics().toString());
    }

    private DialogueCompileResult compile(String basename, String source) {
        return compiler.compile("jdh:echo_dialogue/dialogues/chapter_1/" + basename + ".dialogue.es", source);
    }

    private void assertCode(String source, String code) {
        DialogueCompileResult result = compile("invalid", source);
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)),
                () -> result.diagnostics().toString());
    }
}
