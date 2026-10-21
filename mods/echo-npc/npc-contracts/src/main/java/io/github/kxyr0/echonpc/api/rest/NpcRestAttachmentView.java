package io.github.kxyr0.echonpc.api.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import java.util.Objects;

/**
 * Immutable public snapshot of an authoritative rest attachment.
 */
public record NpcRestAttachmentView(
        ActorId npcId,
        String providerId,
        NpcWorldId worldId,
        BlockPosition bedPosition,
        BlockId expectedBlock,
        BlockPosition canonicalBedPosition,
        NpcPosition sleepAnchor,
        float sleepYaw,
        String pose,
        long attachmentRevision,
        String providerState,
        boolean occupied,
        NpcSceneCommandId commandId
) {
    public NpcRestAttachmentView {
        npcId = Objects.requireNonNull(npcId, "npcId");
        providerId = requireText(providerId, "providerId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        bedPosition = Objects.requireNonNull(bedPosition, "bedPosition");
        expectedBlock = Objects.requireNonNull(expectedBlock, "expectedBlock");
        canonicalBedPosition = Objects.requireNonNull(canonicalBedPosition, "canonicalBedPosition");
        sleepAnchor = Objects.requireNonNull(sleepAnchor, "sleepAnchor");
        pose = requireText(pose, "pose");
        providerState = Objects.requireNonNull(providerState, "providerState");
        commandId = Objects.requireNonNull(commandId, "commandId");
        if (!Float.isFinite(sleepYaw) || attachmentRevision < 0L) {
            throw new IllegalArgumentException("sleepYaw must be finite and revision must not be negative.");
        }
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }
}
