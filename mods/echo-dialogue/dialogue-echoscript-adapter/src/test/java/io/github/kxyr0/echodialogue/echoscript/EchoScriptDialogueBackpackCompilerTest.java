package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class EchoScriptDialogueBackpackCompilerTest {
    @Test
    void canonicalCallsCompileToExternalWaitCommands() {
        String source = """
                namespace EchoDialogue.Sample;

                class BackpackDialogue : DialogueResource
                {
                    private BlockPosition Place = new BlockPosition(39, -3, 100);

                    private void Init()
                    {
                        Id("sample:backpack");
                        Start(Greeting);
                        Alias("Molly", "jdh:molly");
                    }

                    private void Greeting()
                    {
                        Line("Molly", "start");
                        Npc.EquipBackpack(Molly, Position(37, -3, 100));
                        Npc.UnequipBackpack(Molly, Place);
                        Line("Molly", "done");
                    }
                }
                """;

        DialogueCompileResult result = new EchoScriptDialogueCompiler()
                .compile("sample/backpack.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals("echo_npc_sophisticated_backpacks:equip", commands.get(0).type());
        assertEquals("37", commands.get(0).arguments().get("x"));
        assertEquals("jdh:molly", commands.get(0).arguments().get("npc"));
        assertEquals("echo_npc_sophisticated_backpacks:unequip", commands.get(1).type());
        assertEquals("39", commands.get(1).arguments().get("x"));
    }
}
