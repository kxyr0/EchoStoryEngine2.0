package io.github.kxyr0.echoplayernavigationadapter.navigation;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMotor;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMovementBridge;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationService;
import net.minecraft.server.MinecraftServer;

public final class PlayerNavigationCommandBridge implements PlayerActorMovementBridge {
    private final MinecraftServer server;

    public PlayerNavigationCommandBridge(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerActorResult move(PlayerActorMoveRequest request, PlayerActorMotor motor) {
        EchoNavigationServiceBridge.LookupResult lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.status() != EchoNavigationServiceBridge.Status.RESOLVED) {
            return unavailable("Echo Navigation service is unavailable.");
        }
        NavigationService.MoveResult result = lookup.service().orElseThrow().move(navigationRequest(request));
        if (result.status() == NavigationService.Status.ACCEPTED
                || result.status() == NavigationService.Status.WAITING
                || result.status() == NavigationService.Status.DUPLICATE) {
            return motor.start(request);
        }
        return unavailable(result.message());
    }

    @Override
    public PlayerActorResult stop(PlayerActorId actorId, PlayerActorMotor motor) {
        EchoNavigationServiceBridge.LookupResult lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.status() == EchoNavigationServiceBridge.Status.RESOLVED) {
            lookup.service().orElseThrow().stop(PlayerNavigationIds.subject(actorId));
        }
        return motor.stop(actorId);
    }

    private NavigationRequest navigationRequest(PlayerActorMoveRequest request) {
        return new NavigationRequest(PlayerNavigationIds.move(request.commandId()),
                PlayerNavigationIds.command(request.commandId()), PlayerNavigationIds.execution(request.commandId()),
                PlayerNavigationIds.owner(request.ownerId()), PlayerNavigationIds.subject(request.actorId()),
                new NavigationDestination(request.dimensionId(), block(request.destination().x()),
                        block(request.destination().y()), block(request.destination().z())),
                NavigationBackendId.VANILLA, 1L, 0L);
    }

    private int block(double value) {
        return (int) Math.floor(value);
    }

    private PlayerActorResult unavailable(String message) {
        return new PlayerActorResult(PlayerActorStatus.UNAVAILABLE, PlayerActorOutcome.NONE,
                java.util.Optional.of(PlayerActorFailure.PLAYER_ACTOR_NAVIGATION_UNAVAILABLE),
                java.util.Optional.empty(), message);
    }
}
