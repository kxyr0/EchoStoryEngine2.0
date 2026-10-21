package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.ExternalActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions.BlackspaceActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions.PlayerActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.attachment.ItemAttachmentActionIntrinsics;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear.LinearNodeChain;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LinearCharacterActionBinder {
    public static final Set<String> TARGETS = Set.of(
            "Npc.SpawnAt", "Npc.Move", "Npc.Sprint", "Npc.SleepAt", "Npc.WakeUp", "Npc.Interact",
            "Npc.StopInteraction", "Npc.StandUp", "Npc.EquipBackpack", "Npc.UnequipBackpack",
            "Npc.TakeItem", "Npc.MoveAttachedItem", "Npc.DropItem", "Npc.ThrowItem",
            "Player.TakeItem", "Player.MoveAttachedItem", "Player.DropItem", "Player.ThrowItem",
            "Player.SpawnAt", "Player.SetHotbarItem", "Player.SelectHotbarSlot", "Player.SitAt", "Player.StandUp",
            "Blackspace.Create", "Player.Teleport");

    private final BlackspaceActionIntrinsics blackspaceActions;
    private final PlayerActionIntrinsics playerActions;
    private final NpcActionIntrinsics npcActions;
    private final BackpackActionIntrinsics backpackActions;
    private final ItemAttachmentActionIntrinsics itemAttachments;

    public LinearCharacterActionBinder(SourceNodeFactory nodeFactory) {
        blackspaceActions = new BlackspaceActionIntrinsics(nodeFactory);
        playerActions = new PlayerActionIntrinsics(nodeFactory);
        npcActions = new NpcActionIntrinsics(nodeFactory);
        backpackActions = new BackpackActionIntrinsics(nodeFactory);
        itemAttachments = new ItemAttachmentActionIntrinsics(nodeFactory);
    }

    public Optional<LinearNodeChain.Entry> bind(
            IntrinsicCallReader reader,
            SourceSpan span,
            Map<String, String> blackspaceVariables,
            ExternalActionIntrinsics externalActions) {
        if (ItemAttachmentActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return pending(itemAttachments.id(reader), itemAttachments.linear(reader, span));
        }
        if (BackpackActionIntrinsics.TARGETS.contains(reader.targetName())) {
            return pending(backpackActions.id(reader), backpackActions.linear(reader, span));
        }
        if (reader.targetName().startsWith("Npc.")) {
            return npc(reader, span, externalActions);
        }
        return playerWorld(reader, span, blackspaceVariables);
    }

    private Optional<LinearNodeChain.Entry> npc(
            IntrinsicCallReader reader, SourceSpan span, ExternalActionIntrinsics externalActions) {
        return npcActions.linearCommand(reader, span, externalActions)
                .flatMap(action -> pending(action.id(), action.spec()));
    }

    private Optional<LinearNodeChain.Entry> playerWorld(
            IntrinsicCallReader reader, SourceSpan span, Map<String, String> blackspaceVariables) {
        return switch (reader.targetName()) {
            case "Player.SpawnAt" -> pending("spawn_player", playerActions.linearSpawnAt(reader, span));
            case "Player.SetHotbarItem" -> pending(
                    "set_hotbar_item", playerActions.linearSetHotbarItem(reader, span));
            case "Player.SelectHotbarSlot" -> pending(
                    "select_hotbar_slot", playerActions.linearSelectHotbarSlot(reader, span));
            case "Player.SitAt" -> pending("sit_player", playerActions.linearSitAt(reader, span));
            case "Player.StandUp" -> pending("stand_up_player", playerActions.linearStandUp(reader, span));
            case "Blackspace.Create" -> pending("create_blackspace",
                    blackspaceActions.linearCreate(reader, span, Optional.empty()));
            case "Player.Teleport" -> pending("teleport_player",
                    blackspaceActions.linearTeleport(reader, span, blackspaceVariables));
            default -> Optional.empty();
        };
    }

    private Optional<LinearNodeChain.Entry> pending(String id, Optional<NodeSpec> spec) {
        return spec.map(value -> new LinearNodeChain.Entry(id, value));
    }
}
