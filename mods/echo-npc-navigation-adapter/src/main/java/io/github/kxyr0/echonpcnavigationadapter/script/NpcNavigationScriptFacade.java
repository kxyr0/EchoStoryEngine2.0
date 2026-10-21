package io.github.kxyr0.echonpcnavigationadapter.script;

import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationService;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcnavigationadapter.navigation.NpcSubjectIds;
import net.minecraft.server.MinecraftServer;

public final class NpcNavigationScriptFacade {
    public NavigationService.MoveResult move(MinecraftServer server, String executionId,
            ActorId actorId, NavigationDestination destination) {
        return move(server, executionId, new NpcNavigationPayloads.MoveCommand(actorId, destination,
                NavigationGait.WALK, NpcNavigationPayloads.WALK_SPEED_BLOCKS_PER_SECOND, true));
    }

    public NavigationService.MoveResult move(MinecraftServer server, String executionId,
            NpcNavigationPayloads.MoveCommand command) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.service().isEmpty() || lookup.queryService().isEmpty()) {
            return new NavigationService.MoveResult(NavigationService.Status.UNAVAILABLE,
                    java.util.Optional.empty(), java.util.Optional.empty(), "Echo Navigation is unavailable.");
        }
        var settings = lookup.queryService().orElseThrow().settings();
        ActorId actorId = command.actorId();
        NavigationDestination destination = command.destination();
        NavigationRequest request = new NavigationRequest(DeterministicNavigationIds.move(executionId, command),
                DeterministicNavigationIds.command(executionId, command),
                DeterministicNavigationIds.execution(executionId), DeterministicNavigationIds.owner(executionId),
                NpcSubjectIds.subject(actorId), destination, settings.backendId(), settings.revision(), 0L,
                command.gait(), command.speedBlocksPerSecond(), command.synchronizeAnimationSpeed());
        return lookup.service().orElseThrow().move(request);
    }

    public NavigationService.MoveResult stop(MinecraftServer server, ActorId actorId) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) {
            return new NavigationService.MoveResult(NavigationService.Status.UNAVAILABLE,
                    java.util.Optional.empty(), java.util.Optional.empty(), "Echo Navigation is unavailable.");
        }
        return lookup.service().orElseThrow().stop(NpcSubjectIds.subject(actorId));
    }

    public boolean isMoving(MinecraftServer server, ActorId actorId) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        return lookup.queryService().map(query -> query.isMoving(new NavigationSubjectId(actorId.value())))
                .orElse(false);
    }
}
