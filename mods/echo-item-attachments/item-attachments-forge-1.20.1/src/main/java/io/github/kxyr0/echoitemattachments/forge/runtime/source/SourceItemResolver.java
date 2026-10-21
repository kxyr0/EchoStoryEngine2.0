package io.github.kxyr0.echoitemattachments.forge.runtime.source;

import io.github.kxyr0.echoitemattachments.api.command.ItemSelector;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SourceItemResolver {
    public SourceSelection resolve(MinecraftServer server, WorldPosition source, ItemSelector selector) {
        ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(source.dimensionId()));
        ServerLevel level = server.getLevel(key);
        if (level == null) return failure(SourceSelection.Status.DIMENSION_UNAVAILABLE,
                "EIA-SOURCE-DIMENSION", "Source dimension is unavailable.");
        Vec3 centre = new Vec3(source.x(), source.y(), source.z());
        AABB bounds = new AABB(centre, centre).inflate(selector.radius());
        List<ItemEntity> all = level.getEntitiesOfClass(ItemEntity.class, bounds,
                entity -> entity.isAlive() && !entity.getItem().isEmpty());
        if (all.isEmpty()) return failure(SourceSelection.Status.NOT_FOUND,
                "EIA-SOURCE-NOT-FOUND", "No world item exists in the authored source AABB.");
        List<ItemEntity> matching = all.stream().filter(entity -> selector.itemId().isEmpty()
                || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.getItem().getItem())
                .toString().equals(selector.itemId().orElseThrow())).toList();
        if (matching.isEmpty()) return failure(SourceSelection.Status.ITEM_MISMATCH,
                "EIA-SOURCE-ITEM-MISMATCH", "World items exist but none match the expected item id.");
        List<ItemEntity> enough = matching.stream()
                .filter(entity -> entity.getItem().getCount() >= selector.count()).toList();
        if (enough.isEmpty()) return failure(SourceSelection.Status.COUNT_MISMATCH,
                "EIA-SOURCE-COUNT-MISMATCH", "Matching world item count is too small.");
        if (enough.size() != 1) return failure(SourceSelection.Status.AMBIGUOUS,
                "EIA-SOURCE-AMBIGUOUS", "More than one exact world item matches the authored source.");
        return new SourceSelection(SourceSelection.Status.RESOLVED,
                java.util.Optional.of(enough.get(0)), "EIA-SOURCE-RESOLVED", "Source resolved exactly.");
    }

    private SourceSelection failure(SourceSelection.Status status, String code, String message) {
        return new SourceSelection(status, java.util.Optional.empty(), code, message);
    }
}
