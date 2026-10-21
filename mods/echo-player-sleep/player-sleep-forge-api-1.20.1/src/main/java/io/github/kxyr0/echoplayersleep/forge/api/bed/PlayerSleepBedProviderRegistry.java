package io.github.kxyr0.echoplayersleep.forge.api.bed;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface PlayerSleepBedProviderRegistry {
    boolean register(PlayerSleepBedProvider provider);

    Optional<PlayerSleepBedProvider> providerFor(Level level, BlockPos pos);

    List<PlayerSleepBedProvider> providers();
}
