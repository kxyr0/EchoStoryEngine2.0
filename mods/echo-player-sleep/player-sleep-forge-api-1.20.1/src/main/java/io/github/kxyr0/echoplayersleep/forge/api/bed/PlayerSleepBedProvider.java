package io.github.kxyr0.echoplayersleep.forge.api.bed;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepBedSnapshot;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface PlayerSleepBedProvider {
    String providerId();

    boolean supports(Level level, BlockPos pos);

    Optional<PlayerSleepBedSnapshot> snapshot(ServerPlayer player, BlockPos pos);

    boolean confirmsSleeping(ServerPlayer player, PlayerSleepBedSnapshot snapshot);
}
