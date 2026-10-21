package io.github.kxyr0.echoplayeractor.forge.api.seat;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSeatSnapshot;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerActorSeatProvider {
    String providerId();

    boolean supports(ServerPlayer player);

    PlayerActorSeatSnapshot snapshot(ServerPlayer player);

    default PlayerActorResult sitAt(ServerPlayer player, BlockPosition position, int timeoutTicks) {
        return PlayerActorResult.unavailable("Seat provider does not support sitting.");
    }

    PlayerActorResult standUp(ServerPlayer player);
}
