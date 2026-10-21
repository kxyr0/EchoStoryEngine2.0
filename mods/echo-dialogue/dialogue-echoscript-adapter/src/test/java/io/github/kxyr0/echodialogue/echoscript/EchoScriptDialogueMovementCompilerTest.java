package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;

import org.junit.jupiter.api.Test;

class EchoScriptDialogueMovementCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void npcMoveCompilesToExternalDialogueCommand() {
        String source = """
                namespace EchoDialogue.Sample;

                class MoveDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:move");
                        Start(Greeting);
                        Alias("father", "sample:father");
                        Alias("player", "sample:player");
                    }

                    private void Greeting()
                    {
                        Line("father", "go");
                        Npc.Move(father, 200, 64, 180);
                        Line("player", "ok");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/move.dialogue.es", source);

        assertTrue(result.successful());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_navigation:npc_move", command.type());
        assertEquals(1, command.position());
        assertEquals("sample:father", command.arguments().get("npc"));
        assertEquals("200", command.arguments().get("x"));
        assertEquals("minecraft:overworld", command.arguments().get("dimension"));
    }

    @Test
    void npcSprintAcceptsVectorAndExplicitSpeed() {
        String source = """
                namespace EchoDialogue.Sample;

                class SprintDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:sprint");
                        Start(Greeting);
                        Alias("father", "sample:father");
                    }

                    private void Greeting()
                    {
                        Line("father", "run");
                        Npc.Sprint(father, Vector3(200, 64, 180), speed: 5.5);
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/sprint.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("SPRINT", command.arguments().get("gait"));
        assertEquals("200", command.arguments().get("x"));
        assertEquals("5.5", command.arguments().get("speed"));
    }

    @Test
    void npcInteractCompilesToExternalDialogueCommand() {
        String source = """
                namespace EchoDialogue.Sample;

                class InteractDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:interact");
                        Start(Greeting);
                        Alias("Sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        Line("Sasha", "go");
                        Npc.Interact(Sasha, 100, 64, 100);
                        Line("Sasha", "done");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/interact.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_npc_interaction:interact", command.type());
        assertEquals(1, command.position());
        assertEquals("jdh:sasha", command.arguments().get("npc"));
        assertEquals("100", command.arguments().get("x"));
        assertEquals("64", command.arguments().get("y"));
        assertEquals("100", command.arguments().get("z"));
        assertEquals("AUTO", command.arguments().get("face"));
        assertEquals("MAIN_HAND", command.arguments().get("hand"));
    }

    @Test
    void npcInteractAcceptsBlockPositionField() {
        String source = """
                namespace EchoDialogue.Sample;

                class InteractPositionDialogue : DialogueResource
                {
                    private BlockPosition TargetBlock = new BlockPosition(100, 64, 100);

                    private void Init()
                    {
                        Id("sample:interact_position");
                        Start(Greeting);
                        Alias("Sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        Line("Sasha", "go");
                        Npc.Interact(Sasha, TargetBlock, face: AUTO, hand: MAIN_HAND);
                        Npc.StopInteraction(Sasha);
                        Npc.StandUp(Sasha);
                        Line("Sasha", "done");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/interact_position.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals("echo_npc_interaction:interact", commands.get(0).type());
        assertEquals("jdh:sasha", commands.get(0).arguments().get("npc"));
        assertEquals("100", commands.get(0).arguments().get("x"));
        assertEquals("64", commands.get(0).arguments().get("y"));
        assertEquals("100", commands.get(0).arguments().get("z"));
        assertEquals("echo_npc_interaction:stop", commands.get(1).type());
        assertEquals("jdh:sasha", commands.get(1).arguments().get("npc"));
        assertEquals("echo_npc_interaction:stand_up", commands.get(2).type());
        assertEquals("jdh:sasha", commands.get(2).arguments().get("npc"));
    }

    @Test
    void lineAfterMoveCompilesArrivalWaitBeforeOnlyThatLine() {
        String source = """
                namespace EchoDialogue.Sample;

                class MoveDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:move_lines");
                        Start(Greeting);
                        Alias("father", "sample:father");
                    }

                    private void Greeting()
                    {
                        Line("father", "go");
                        Npc.Move(father, 200, 64, 180);
                        Line("father", "walking");
                        LineAfterMove("father", "arrived");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/move_lines.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var node = result.dialogue().orElseThrow().startNode();
        assertEquals(3, node.lines().size());
        assertEquals("walking", node.lines().get(1).text());
        assertEquals("arrived", node.lines().get(2).text());
        var commands = node.commands();
        assertEquals(2, commands.size());
        assertEquals("echo_navigation:npc_move", commands.get(0).type());
        assertEquals(1, commands.get(0).position());
        assertEquals("echo_navigation:npc_move_arrival", commands.get(1).type());
        assertEquals(2, commands.get(1).position());
        assertEquals("sample:father", commands.get(1).arguments().get("npc"));
    }


}

