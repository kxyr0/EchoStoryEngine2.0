package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;

import org.junit.jupiter.api.Test;

class EchoScriptDialogueActorCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void cutsceneCommandsCompileToExternalDialogueCommands() {
        String source = cutsceneSource();

        DialogueCompileResult result = compiler.compile("sample/cutscene.dialogue.es", source);

        assertTrue(result.successful());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(4, commands.size());
        assertEquals("echo_cutscene:start", commands.get(0).type());
        assertEquals("jdh:chapter_1/bedtime_story", commands.get(0).arguments().get("cutscene"));
        assertEquals(1, commands.get(0).position());
        assertEquals("echo_cutscene:wait_prepared", commands.get(1).type());
        assertEquals("jdh:chapter_1/bedtime_story", commands.get(1).arguments().get("cutscene"));
        assertEquals("echo_screen:set_black", commands.get(2).type());
        assertEquals("0", commands.get(2).arguments().get("canvasGroup"));
        assertEquals("300", commands.get(2).arguments().get("time"));
        assertEquals("echo_cutscene:wait", commands.get(3).type());
        assertEquals("jdh:chapter_1/bedtime_story", commands.get(3).arguments().get("cutscene"));
    }

    @Test
    void playerActorCommandsCompileToExternalDialogueCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class PlayerActorDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:player_actor");
                        Start(Greeting);
                        Alias("player", "sample:player");
                    }

                    private void Greeting()
                    {
                        Line("player", "start");
                        Player.LookAt(124, 65, 190);
                        Line("player", "switch");
                        Player.Interact(124, 65, 190, face: WEST);
                        Line("player", "leave");
                        Player.StandUp();
                        Line("player", "go");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/player_actor.dialogue.es", source);

        assertTrue(result.successful());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(3, commands.size());
        assertEquals("echo_player_actor:look_at", commands.get(0).type());
        assertEquals("124", commands.get(0).arguments().get("x"));
        assertEquals("echo_player_actor:interact", commands.get(1).type());
        assertEquals("WEST", commands.get(1).arguments().get("face"));
        assertEquals("echo_player_actor:stand_up", commands.get(2).type());
    }

    @Test
    void playerFatigueCommandsCompileToExternalDialogueCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class PlayerVitalsDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:player_vitals");
                        Start(Greeting);
                        Alias("player", "sample:player");
                    }

                    private void Greeting()
                    {
                        Line("player", "start");
                        Player.SetFatigue(50);
                        AddFatigue(10);
                        Player.ReduceFatigue(value: 5);
                        RestoreFatigue();
                        Line("player", "done");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/player_vitals.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(4, commands.size());
        assertEquals("echo_player_vitals:set_fatigue", commands.get(0).type());
        assertEquals("50", commands.get(0).arguments().get("value"));
        assertEquals("echo_player_vitals:add_fatigue", commands.get(1).type());
        assertEquals("10", commands.get(1).arguments().get("value"));
        assertEquals("echo_player_vitals:reduce_fatigue", commands.get(2).type());
        assertEquals("5", commands.get(2).arguments().get("value"));
        assertEquals("echo_player_vitals:restore_fatigue", commands.get(3).type());
    }

    @Test
    void effectApplyToPlayerCompilesToExternalDialogueCommand() {
        String source = """
                namespace EchoDialogue.Sample;

                class EffectDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:effect");
                        Start(Greeting);
                        Alias("player", "sample:player");
                    }

                    private void Greeting()
                    {
                        Effect.ApplyToPlayer("jdh:body/head_spinning", intensity: 0.5);
                        Line("player", "dizzy");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/effect.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_effects:apply_to_player", command.type());
        assertEquals(0, command.position());
        assertEquals("jdh:body/head_spinning", command.arguments().get("effectId"));
        assertEquals("0.5", command.arguments().get("intensity"));
    }

    @Test
    void standaloneFaceAnimationCommandCompilesToExternalDialogueCommand() {
        String source = """
                namespace EchoDialogue.Sample;

                class FaceCommandDialogue : DialogueResource
                {
                    private FaceAnimationId SashaSleep = "jdh:sasha/sleep";

                    private void Init()
                    {
                        Id("sample:face_command");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        SetFaceAnimation(sasha, SashaSleep);
                        Line("sasha", "sleeping");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/face_command.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_animation:set_face_animation", command.type());
        assertEquals(0, command.position());
        assertEquals("jdh:sasha", command.arguments().get("subject"));
        assertEquals("jdh:sasha/sleep", command.arguments().get("animation"));
    }


    private static String cutsceneSource() {
        return """
                namespace EchoDialogue.Sample;

                class CutsceneDialogue : DialogueResource
                {
                    private CutsceneId BedtimeStory = "jdh:chapter_1/bedtime_story";

                    private void Init()
                    {
                        Id("sample:cutscene");
                        Start(Greeting);
                        Alias("father", "sample:father");
                    }

                    private void Greeting()
                    {
                        Line("father", "start");
                        Cutscene.Start(BedtimeStory);
                        Line("father", "wait");
                        Cutscene.WaitPrepared(BedtimeStory);
                        Screen.SetBlack(0, 300);
                        Line("father", "prepared");
                        Cutscene.Wait(id: "jdh:chapter_1/bedtime_story");
                        Line("father", "done");
                    }
                }
                """;
    }

}

