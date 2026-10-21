package io.github.kxyr0.echodialogue.echoscript;

import static io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueSources.validSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;

import org.junit.jupiter.api.Test;

class EchoScriptDialogueCommandCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void delayCompilesBetweenLinesAndCanBeRepeated() {
        String source = """
                namespace EchoDialogue.Sample;

                class DelayedDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:delayed_lines");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        Npc.Move(sasha, 200, 64, 180);
                        LineAfterMove("sasha", "Па, ты хоть сегодня выйдешь?");
                        Delay(40);
                        Line("sasha", "Я подожду.");
                        Delay(ticks: 20);
                        Line("sasha", "Па?");
                        Complete("jdh:asked_okay");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/delayed_lines.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var node = result.dialogue().orElseThrow().startNode();
        assertEquals(3, node.lines().size());
        var commands = node.commands();
        assertEquals(4, commands.size());
        assertEquals("echo_navigation:npc_move", commands.get(0).type());
        assertEquals("echo_navigation:npc_move_arrival", commands.get(1).type());
        assertEquals("echo_dialogue:delay", commands.get(2).type());
        assertEquals(1, commands.get(2).position());
        assertEquals("40", commands.get(2).arguments().get("ticks"));
        assertEquals("echo_dialogue:delay", commands.get(3).type());
        assertEquals(2, commands.get(3).position());
        assertEquals("20", commands.get(3).arguments().get("ticks"));
    }

    @Test
    void delayRejectsNonPositiveTicks() {
        String source = validSource().replace("Line(\"player\", \"Гулял.\");",
                "Delay(0);\n        Line(\"player\", \"Гулял.\");");

        DialogueCompileResult result = compiler.compile("sample/invalid_delay.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("EDLG-SEM-074")));
    }

    @Test
    void npcMoveCompilesInLegacyNodeDefAuthoring() {
        String source = legacyMoveSource();

        DialogueCompileResult result = compiler.compile("sample/legacy_move.dialogue.es", source);

        assertTrue(result.successful());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_navigation:npc_move", command.type());
        assertEquals("sample:father", command.arguments().get("npc"));
    }

    @Test
    void npcSetFormCompilesToExternalDialogueCommand() {
        String source = """
                namespace EchoDialogue.Sample;

                class SetFormDialogue : DialogueResource
                {
                    private NpcFormId AdultSasha = "jdh:sasha/adult";

                    private void Init()
                    {
                        Id("sample:set_form");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        Line("sasha", "small");
                        Npc.SetForm(sasha, AdultSasha);
                        Line("sasha", "adult");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/set_form.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        CompiledDialogueCommand command = result.dialogue().orElseThrow().startNode().commands().get(0);
        assertEquals("echo_npc:set_form", command.type());
        assertEquals(1, command.position());
        assertEquals("jdh:sasha", command.arguments().get("npc"));
        assertEquals("jdh:sasha/adult", command.arguments().get("form"));
    }

    @Test
    void npcRestCommandsCompileToExternalDialogueCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class RestDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("sample:rest");
                        Start(Greeting);
                        Alias("sasha", "jdh:sasha");
                    }

                    private void Greeting()
                    {
                        Line("sasha", "small");
                        Npc.WakeUp(sasha);
                        Npc.SleepAt(sasha, 49, -3, 92);
                        Line("sasha", "sleeping");
                    }
                }
                """;

        DialogueCompileResult result = compiler.compile("sample/rest.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(2, commands.size());
        assertEquals("echo_npc:wake_up", commands.get(0).type());
        assertEquals("jdh:sasha", commands.get(0).arguments().get("npc"));
        assertEquals("echo_npc:sleep_at", commands.get(1).type());
        assertEquals("49", commands.get(1).arguments().get("bedX"));
        assertEquals("-3", commands.get(1).arguments().get("bedY"));
        assertEquals("92", commands.get(1).arguments().get("bedZ"));
        assertEquals("minecraft:overworld", commands.get(1).arguments().get("worldId"));
        assertEquals("echo_npc:any_bed", commands.get(1).arguments().get("expectedBlock"));
    }


    private static String legacyMoveSource() {
        return """
                namespace EchoDialogue.Sample;

                class MoveDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Leaving() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:move");
                    }

                    private void Init()
                    {
                        Id("sample:move");
                        Start("leaving");
                        Alias("father", "sample:father");
                        Alias("player", "sample:player");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                    private NodeDef Leaving()
                    {
                        var node = new Node("leaving");
                        Line("father", "go");
                        Npc.Move(father, 200, 64, 180);
                        Line("player", "ok");
                        return node;
                    }
                }
                """;
    }

}

