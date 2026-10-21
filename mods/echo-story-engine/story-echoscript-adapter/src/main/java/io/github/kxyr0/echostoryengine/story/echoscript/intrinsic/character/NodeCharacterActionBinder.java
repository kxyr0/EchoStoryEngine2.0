package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions.BlackspaceActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions.PlayerActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.attachment.ItemAttachmentActionIntrinsics;
import java.util.Optional;
import java.util.Set;

public final class NodeCharacterActionBinder {
    public static final Set<String> TARGETS = Set.of(
            "Npc.Move", "Npc.Sprint", "Npc.Stop", "Npc.Interact", "Npc.StopInteraction", "Npc.StandUp",
            "Npc.EquipBackpack", "Npc.UnequipBackpack",
            "Npc.TakeItem", "Npc.MoveAttachedItem", "Npc.DropItem", "Npc.ThrowItem",
            "Player.TakeItem", "Player.MoveAttachedItem", "Player.DropItem", "Player.ThrowItem",
            "Player.SpawnAt", "Player.SetHotbarItem", "Player.SelectHotbarSlot", "Player.SitAt", "Player.StandUp",
            "Blackspace.Create", "Player.Teleport");

    private final BlackspaceActionIntrinsics blackspaceActions;
    private final PlayerActionIntrinsics playerActions;
    private final NpcActionIntrinsics npcActions;
    private final BackpackActionIntrinsics backpackActions;
    private final ItemAttachmentActionIntrinsics itemAttachments;

    public NodeCharacterActionBinder(SourceNodeFactory nodeFactory) {
        blackspaceActions = new BlackspaceActionIntrinsics(nodeFactory);
        playerActions = new PlayerActionIntrinsics(nodeFactory);
        npcActions = new NpcActionIntrinsics(nodeFactory);
        backpackActions = new BackpackActionIntrinsics(nodeFactory);
        itemAttachments = new ItemAttachmentActionIntrinsics(nodeFactory);
    }

    public Optional<SourceNode> bind(IntrinsicCallReader reader, SourceSpan span) {
        if (reader.targetName().startsWith("Npc.") && !BackpackActionIntrinsics.TARGETS.contains(reader.targetName())
                && !ItemAttachmentActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return npc(reader, span);
        }
        if (BackpackActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return backpackActions.node(reader, span);
        }
        if (ItemAttachmentActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return itemAttachments.node(reader, span);
        }
        return playerWorld(reader, span);
    }

    private Optional<SourceNode> npc(IntrinsicCallReader reader, SourceSpan span) {
        return switch (reader.targetName()) {
            case "Npc.Move" -> npcActions.nodeMove(reader, span);
            case "Npc.Sprint" -> npcActions.nodeSprint(reader, span);
            case "Npc.Stop" -> npcActions.nodeStop(reader, span);
            case "Npc.Interact" -> npcActions.nodeInteract(reader, span);
            case "Npc.StopInteraction" -> npcActions.nodeStopInteraction(reader, span);
            case "Npc.StandUp" -> npcActions.nodeStandUp(reader, span);
            default -> Optional.empty();
        };
    }

    private Optional<SourceNode> playerWorld(IntrinsicCallReader reader, SourceSpan span) {
        return switch (reader.targetName()) {
            case "Player.SpawnAt" -> playerActions.spawnAt(reader, span);
            case "Player.SetHotbarItem" -> playerActions.setHotbarItem(reader, span);
            case "Player.SelectHotbarSlot" -> playerActions.selectHotbarSlot(reader, span);
            case "Player.SitAt" -> playerActions.sitAt(reader, span);
            case "Player.StandUp" -> playerActions.standUp(reader, span);
            case "Blackspace.Create" -> blackspaceActions.createNode(reader, span);
            case "Player.Teleport" -> blackspaceActions.teleportNode(reader, span);
            default -> Optional.empty();
        };
    }
}
