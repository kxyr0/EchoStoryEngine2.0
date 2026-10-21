package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import org.junit.jupiter.api.Test;

final class EchoScriptItemAttachmentCompilerTest {
    @Test
    void npcAndPlayerCallsCompileThroughTheRealStoryBinder() {
        var result = EchoScriptAdapterFixture.compile("fixtures/item_attachments.story.es", """
                namespace JDH.Chapter1.Home;

                class ItemStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:item_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Npc.TakeItem(Molly, Position(100, 64, 100), hand: RIGHT_HAND,
                            expected: "minecraft:book", count: 1, animation: TakeBook);
                        Npc.MoveAttachedItem(Molly, from: RIGHT_HAND, to: LEFT_HAND, animation: SwapBook);
                        Npc.ThrowItem(Molly, hand: LEFT_HAND, animation: ThrowBook, release: HAND_MOTION);
                        Player.TakeItem(Position(101, 64, 100), hand: RIGHT_HAND,
                            expected: "minecraft:book", animation: TakeBook);
                        Player.DropItem(hand: RIGHT_HAND);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        var story = result.compiledStory().orElseThrow();
        var take = story.find(NodeId.of("npc_take_item")).orElseThrow();
        assertEquals(NamespacedId.of("echo_item_attachments:take"), take.actionType());
        assertEquals("Molly", take.actionArguments().get("npc"));
        assertEquals("Position(100,64,100)", take.actionArguments().get("position"));
        assertEquals("RIGHT_HAND", take.actionArguments().get("hand"));
        var player = story.find(NodeId.of("player_take_item")).orElseThrow();
        assertEquals("player", player.actionArguments().get("player"));
        assertEquals(NamespacedId.of("echo_item_attachments:take"), player.actionType());
    }
}
