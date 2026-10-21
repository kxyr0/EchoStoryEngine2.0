package io.github.kxyr0.echoplayeractor.forge.seat;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.position.BlockPosition;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;
import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProvider;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerSitCommand {
    private static final int NEARBY_SEARCH_RADIUS = 1;

    private final ServerPlayerBodyResolver resolver;
    private final List<PlayerActorSeatProvider> providers;

    public PlayerSitCommand(ServerPlayerBodyResolver resolver, List<PlayerActorSeatProvider> providers) {
        this.resolver = resolver;
        this.providers = providers;
    }

    public PlayerActorResult sitAt(PlayerSitRequest request) {
        return resolver.resolve(request.actorId()).map(player -> sitAt(player, request))
                .orElseGet(() -> PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE,
                        "Player actor is offline."));
    }

    private PlayerActorResult sitAt(ServerPlayer player, PlayerSitRequest request) {
        List<PlayerActorSeatProvider> supported = providers.stream()
                .filter(provider -> provider.supports(player))
                .toList();
        if (supported.isEmpty()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_PROVIDER_NOT_FOUND,
                    "No registered seat provider accepted this player.");
        }
        if (supported.size() > 1) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_PROVIDER_CONFLICT,
                    "More than one seat provider accepted this player.");
        }
        PlayerActorSeatProvider provider = supported.get(0);
        PlayerActorResult result = trySitAt(player, provider, request.chairPosition(), request.timeoutTicks());
        if (seatNotFound(result)) {
            return nearbySeat(player, provider, request).orElse(result);
        }
        return result;
    }

    private Optional<PlayerActorResult> nearbySeat(
            ServerPlayer player,
            PlayerActorSeatProvider provider,
            PlayerSitRequest request) {
        BlockPosition origin = request.chairPosition();
        PlayerActorResult firstSpecificFailure = null;
        for (int distance = 1; distance <= NEARBY_SEARCH_RADIUS * 3; distance++) {
            for (int x = -NEARBY_SEARCH_RADIUS; x <= NEARBY_SEARCH_RADIUS; x++) {
                for (int y = -NEARBY_SEARCH_RADIUS; y <= NEARBY_SEARCH_RADIUS; y++) {
                    for (int z = -NEARBY_SEARCH_RADIUS; z <= NEARBY_SEARCH_RADIUS; z++) {
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) != distance) {
                            continue;
                        }
                        PlayerActorResult result = trySitAt(player, provider, offset(origin, x, y, z),
                                request.timeoutTicks());
                        if (result.status() == PlayerActorStatus.SUCCEEDED) {
                            return Optional.of(result);
                        }
                        if (!seatNotFound(result) && firstSpecificFailure == null) {
                            firstSpecificFailure = result;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(firstSpecificFailure);
    }

    private PlayerActorResult trySitAt(
            ServerPlayer player,
            PlayerActorSeatProvider provider,
            BlockPosition position,
            int timeoutTicks) {
        BlockPos block = new BlockPos(position.x(), position.y(), position.z());
        if (!player.serverLevel().isLoaded(block) || player.serverLevel().getBlockState(block).isAir()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_NOT_FOUND,
                    "Seat block was not found at " + position + ".");
        }
        PlayerActorResult result = provider.sitAt(player, position, timeoutTicks);
        if (result.status() == PlayerActorStatus.SUCCEEDED && !provider.snapshot(player).seated()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SIT_REJECTED,
                    "Seat provider did not confirm seated state.");
        }
        return result.status() == PlayerActorStatus.UNAVAILABLE
                ? PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_SEAT_PROVIDER_NOT_FOUND,
                        "Seat provider does not support SitAt.")
                : result;
    }

    private boolean seatNotFound(PlayerActorResult result) {
        return result.failure().filter(value -> value == PlayerActorFailure.PLAYER_ACTOR_SEAT_NOT_FOUND).isPresent();
    }

    private BlockPosition offset(BlockPosition origin, int x, int y, int z) {
        return new BlockPosition(origin.x() + x, origin.y() + y, origin.z() + z);
    }
}
