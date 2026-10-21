package io.github.kxyr0.echoiteminspection.client.world;

import io.github.kxyr0.echoiteminspection.api.ItemStackSnapshot;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Immutable boundary. No IPLA object or live ItemStack escapes the resolver. */
public record ItemInspectionWorldTarget(Identity identity, ItemStackSnapshot snapshot,
        Vec3 hitPosition, double distance, String validityToken) {
    public record Identity(ResourceLocation dimension, BlockPos position, int slot, UUID incarnation) {
        public Identity {
            Objects.requireNonNull(dimension);
            position = Objects.requireNonNull(position).immutable();
            Objects.requireNonNull(incarnation);
            if (slot < 0) throw new IllegalArgumentException("Negative world slot");
        }
    }

    public ItemInspectionWorldTarget {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(hitPosition);
        Objects.requireNonNull(validityToken);
        if (!Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("Invalid hit distance");
    }

    public ItemInspectionSourceKind sourceKind() { return ItemInspectionSourceKind.IPLA_WORLD; }

    public boolean sameRevision(ItemInspectionWorldTarget other) {
        return other != null && identity.equals(other.identity) && validityToken.equals(other.validityToken);
    }
}
