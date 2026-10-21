package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;

import org.junit.jupiter.api.Test;

class EchoScriptDialogueAnimationCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void faceAnimationClearCommandsCompileToExternalDialogueCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class FaceClearDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:face_clear");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        ClearFaceAnimations();
                        ClearFaceAnimation(sasha);
                        Line("sasha", "neutral");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/face_clear.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(2, commands.size());
        assertEquals("echo_animation:clear_all_face_animations", commands.get(0).type());
        assertEquals(0, commands.get(0).position());
        assertTrue(commands.get(0).arguments().isEmpty());
        assertEquals("echo_animation:clear_face_animation", commands.get(1).type());
        assertEquals(0, commands.get(1).position());
        assertEquals("jdh:sasha", commands.get(1).arguments().get("subject"));
    }

    @Test
    void blinkTimingCommandsCompileToExternalDialogueCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class BlinkTimingDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:blink_timing");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        SetBlinkTiming(sasha, periodTicks: 34, closeTicks: 5, holdTicks: 8, openTicks: 9);
                        Line("sasha", "sleepy");
                        ClearBlinkTiming(sasha);
                        Line("sasha", "awake");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/blink_timing.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(2, commands.size());
        assertEquals("echo_animation:set_blink_timing", commands.get(0).type());
        assertEquals(0, commands.get(0).position());
        assertEquals("jdh:sasha", commands.get(0).arguments().get("subject"));
        assertEquals("34", commands.get(0).arguments().get("periodTicks"));
        assertEquals("5", commands.get(0).arguments().get("closeTicks"));
        assertEquals("8", commands.get(0).arguments().get("holdTicks"));
        assertEquals("9", commands.get(0).arguments().get("openTicks"));
        assertEquals("echo_animation:clear_blink_timing", commands.get(1).type());
        assertEquals(1, commands.get(1).position());
        assertEquals("jdh:sasha", commands.get(1).arguments().get("subject"));
    }

    @Test
    void blinkTimingCommandRejectsInvalidActiveTicks() {
        String source = """
                namespace EchoDialogue.Sample;

                class BlinkTimingDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:blink_timing");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        SetBlinkTiming(sasha, 10, 5, 4, 1);
                        Line("sasha", "sleepy");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/blink_timing_invalid.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-057")));
    }

    @Test
    void cutsceneStopCanCompileAfterLastLineBeforeCompletion() {
        String source = """
                namespace EchoDialogue.Sample;

                class StopAfterLineDialogue : DialogueResource
                {
                    private CutsceneId Scene = "jdh:test_scene";

                    private void Init()
                    {
                        Id("sample:stop_after_line");
                        Start(Greeting);
                        Alias("john", "jdh:john");
                    }

                    private void Greeting()
                    {
                        Line("john", "last");
                        Cutscene.Stop(Scene);
                        Complete("sample:done");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/stop_after_line.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_cutscene:stop", command.type());
        assertEquals(1, command.position());
        assertEquals("jdh:test_scene", command.arguments().get("cutscene"));
    }


}

