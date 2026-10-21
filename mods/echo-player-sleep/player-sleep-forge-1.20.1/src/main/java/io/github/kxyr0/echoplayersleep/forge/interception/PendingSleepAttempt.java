package io.github.kxyr0.echoplayersleep.forge.interception;

import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import net.minecraft.core.BlockPos;

record PendingSleepAttempt(PlayerRef player, BlockPos bedPos, long startedTick, boolean narrativeCandidate) {
}
