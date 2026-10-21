package io.github.kxyr0.echoseating.forge.runtime;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorConfidence;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorSource;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.Vector3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeSeatAnchorResolver {
    public SeatAnchor resolve(ServerPlayer player, Entity vehicle, long serverTick, long revision) {
        Vector3 position = new Vector3(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
        return new SeatAnchor(new DimensionId(player.level().dimension().location().toString()),
                position, new BlockPosition(vehicle.blockPosition().getX(), vehicle.blockPosition().getY(),
                vehicle.blockPosition().getZ()), SeatAnchorSource.VEHICLE_BLOCK_POSITION,
                SeatAnchorConfidence.MEDIUM, vehicle.getUUID(), String.valueOf(type), serverTick,
                new SeatingRevision(revision));
    }
}
