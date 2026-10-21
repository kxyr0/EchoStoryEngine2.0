package io.github.kxyr0.echoiteminspection.client.integration.ipla;

import com.akicater.blocks.LayingItemEntity;
import io.github.kxyr0.echoiteminspection.api.ItemStackSnapshot;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionWorldTarget;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Keeps expensive snapshot/fingerprint work off the unchanged-target render path. */
final class IplaTargetSnapshotCache {
    private LayingItemEntity entity;
    private int slot = -1;
    private ItemStack stack = ItemStack.EMPTY;
    private ItemStackSnapshot snapshot;
    private ItemInspectionWorldTarget.Identity identity;
    private Vec3 hit;
    private float rotation;
    private boolean quad;
    private String revision;

    int unchangedSlot(LayingItemEntity candidate, Vec3 candidateHit) {
        return entity == candidate && hit != null && hit.distanceToSqr(candidateHit) < 1.0E-12
                && slot >= 0 && quad == entity.quad.get(slot / 4)
                && ItemStack.matches(stack, entity.inv.get(slot)) ? slot : -1;
    }

    ItemInspectionWorldTarget capture(LayingItemEntity candidate, int candidateSlot, Vec3 candidateHit,
            double distance, ResourceLocation dimension) {
        boolean newIdentity = entity != candidate || slot != candidateSlot;
        ItemStack current = candidate.inv.get(candidateSlot);
        boolean changed = newIdentity || !ItemStack.matches(stack, current);
        if (newIdentity) identity = new ItemInspectionWorldTarget.Identity(dimension,
                candidate.getBlockPos(), candidateSlot, UUID.randomUUID());
        if (changed) {
            stack = current.copy();
            snapshot = new ItemStackSnapshot(current);
        }
        float nextRotation = candidate.rot.get(candidateSlot);
        boolean nextQuad = candidate.quad.get(candidateSlot / 4);
        if (changed || rotation != nextRotation || quad != nextQuad) {
            revision = snapshot.fingerprint() + ":" + nextRotation + ":" + nextQuad;
        }
        entity = candidate;
        slot = candidateSlot;
        hit = candidateHit;
        rotation = nextRotation;
        quad = nextQuad;
        return new ItemInspectionWorldTarget(identity, snapshot, hit, distance, revision);
    }

    void clear() {
        entity = null;
        slot = -1;
        hit = null;
        identity = null;
        stack = ItemStack.EMPTY;
        snapshot = null;
        revision = null;
    }
}
