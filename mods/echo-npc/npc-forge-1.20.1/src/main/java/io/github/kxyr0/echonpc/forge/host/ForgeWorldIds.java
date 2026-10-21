package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Converts between neutral NpcWorldId and Forge dimension keys at the platform boundary.
 */
public final class ForgeWorldIds {
    private ForgeWorldIds() {
    }

    public static NpcWorldId fromLevel(ServerLevel level) {
        return new NpcWorldId(level.dimension().location().toString());
    }

    public static ResourceKey<Level> toKey(NpcWorldId worldId) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(worldId.value()));
    }
}
