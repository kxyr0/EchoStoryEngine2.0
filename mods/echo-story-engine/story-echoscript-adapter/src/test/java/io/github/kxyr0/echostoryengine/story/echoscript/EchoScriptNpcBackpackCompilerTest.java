package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import org.junit.jupiter.api.Test;

final class EchoScriptNpcBackpackCompilerTest {
    @Test
    void canonicalCallsCompileToBlockingExternalActions() {
        var result = EchoScriptAdapterFixture.compile("fixtures/backpack_story.es", """
                namespace JDH.Chapter1.Home;

                class BackpackStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:backpack_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Npc.EquipBackpack("jdh:molly", Position(37, -3, 100));
                        Npc.UnequipBackpack("jdh:molly", Position(39, -3, 100));
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode equip = story.find(NodeId.of("equip_backpack")).orElseThrow();
        StoryNode unequip = story.find(NodeId.of("unequip_backpack")).orElseThrow();
        assertEquals(NamespacedId.of("echo_npc_sophisticated_backpacks:equip"), equip.actionType());
        assertEquals("37", equip.actionArguments().get("x"));
        assertEquals(NodeId.of("unequip_backpack"), equip.nextNode());
        assertEquals(NamespacedId.of("echo_npc_sophisticated_backpacks:unequip"), unequip.actionType());
        assertEquals("39", unequip.actionArguments().get("x"));
    }
}
