package io.github.kxyr0.echodialogue.echoscript;

import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactDialogueAuthoringTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void methodNodesCompileWithLineCuesAndGoTo() {
        DialogueCompileResult result = compiler.compile("sample/compact.dialogue.es", compactSource());

        assertTrue(result.successful());
        CompiledDialogue dialogue = result.dialogue().orElseThrow();
        CompiledDialogueLine line = dialogue.lines().get(0);

        assertEquals("sample:compact", dialogue.id().toString());
        assertEquals("greeting", dialogue.startNodeId().toString());
        assertEquals("sample.compact.line_1", line.key().orElseThrow());
        assertTrue(line.thought());
        assertTrue(line.cues().contains(new CompiledDialogueLine.Cue("faceAnimation", "sample:angry")));
        assertTrue(line.cues().contains(new CompiledDialogueLine.Cue("bodyAnimation", "sample:wave")));
        assertEquals("done", dialogue.startNode().transition().orElseThrow()
                .targetNodeId().orElseThrow().toString());
    }

    @Test
    void choiceCanTargetMethodReference() {
        DialogueCompileResult result = compiler.compile("sample/choice.dialogue.es", compactChoiceSource());

        assertTrue(result.successful());
        assertEquals("done", result.dialogue().orElseThrow().startNode()
                .choices().get(0).targetNodeId().toString());
    }

    @Test
    void legacyAuthoringProducesCompatibilityWarning() {
        DialogueCompileResult result = compiler.compile("sample/legacy.dialogue.es", legacySource());

        assertTrue(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("DIALOGUE_LEGACY_AUTHORING_SYNTAX")));
    }

    @Test
    void wrongAnimationIdTypeIsRejectedBySetter() {
        String source = compactSource().replace(".SetAnimation(Wave)", ".SetAnimation(AngryFace)");

        DialogueCompileResult result = compiler.compile("sample/wrong_animation.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("DIALOGUE_WRONG_ANIMATION_ID_TYPE")));
    }

    @Test
    void duplicateAnimationSetterIsRejected() {
        String source = compactSource().replace(".SetAnimation(Wave);",
                ".SetAnimation(Wave).SetAnimation(Wave);");

        DialogueCompileResult result = compiler.compile("sample/duplicate_animation.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("DIALOGUE_DUPLICATE_BODY_ANIMATION")));
    }

    @Test
    void faceAnimationClearCompilesAsExplicitCue() {
        String source = compactSource().replace(".SetAnimationFace(AngryFace)", ".SetAnimationFaceClear()");

        DialogueCompileResult result = compiler.compile("sample/clear_face.dialogue.es", source);

        assertTrue(result.successful());
        assertTrue(result.dialogue().orElseThrow().lines().get(0).cues()
                .contains(new CompiledDialogueLine.Cue("faceAnimationClear", "true")));
    }

    @Test
    void faceAnimationClearConflictsWithFaceAnimationSetter() {
        String source = compactSource().replace(".SetAnimationFace(AngryFace)",
                ".SetAnimationFace(AngryFace).SetAnimationFaceClear()");

        DialogueCompileResult result = compiler.compile("sample/duplicate_face.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("DIALOGUE_DUPLICATE_FACE_ANIMATION")));
    }

    @Test
    void faceAnimationStopCompilesAsExplicitCue() {
        String source = compactSource().replace(".SetAnimationFace(AngryFace)", ".StopFaceAnimation()");

        DialogueCompileResult result = compiler.compile("sample/stop_face.dialogue.es", source);

        assertTrue(result.successful());
        assertTrue(result.dialogue().orElseThrow().lines().get(0).cues()
                .contains(new CompiledDialogueLine.Cue("faceAnimationStop", "true")));
    }

    @Test
    void faceAnimationStopConflictsWithFaceAnimationSetter() {
        String source = compactSource().replace(".SetAnimationFace(AngryFace)",
                ".SetAnimationFace(AngryFace).StopFaceAnimation()");

        DialogueCompileResult result = compiler.compile("sample/duplicate_stop_face.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("DIALOGUE_DUPLICATE_FACE_ANIMATION")));
    }

    private static String compactSource() {
        return """
                namespace EchoDialogue.Sample;

                class CompactDialogue : DialogueResource
                {
                    private BodyAnimationId Wave = "sample:wave";
                    private FaceAnimationId AngryFace = "sample:angry";

                    private void Init()
                    {
                        Id("sample:compact");
                        Start(Greeting);
                        Alias("father", "sample:father", display: "Father");
                    }

                    private void Greeting()
                    {
                        Line("father", "Text", "sample.compact.line_1", thought: true)
                            .SetAnimationFace(AngryFace)
                            .SetAnimation(Wave);
                        Dialogue.GoTo(Done);
                    }

                    private void Done()
                    {
                        Complete("sample:done");
                    }
                }
                """;
    }

    private static String compactChoiceSource() {
        return """
                namespace EchoDialogue.Sample;

                class CompactChoiceDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:choice");
                        Start(Question);
                        Alias("father", "sample:father");
                    }

                    private void Question()
                    {
                        Line("father", "Choose.");
                        Choice("done", "Done.", Done);
                    }

                    private void Done()
                    {
                        Line("father", "Done.");
                    }
                }
                """;
    }

    private static String legacySource() {
        return """
                class LegacyDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Greeting() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:legacy");
                    }

                    private void Init()
                    {
                        Start("greeting");
                        Alias("father", "sample:father");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Greeting()
                    {
                        var node = new Node("greeting");
                        Line("father", "Hello.");
                        return node;
                    }
                }
                """;
    }
}
