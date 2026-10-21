package io.github.kxyr0.echonpcinteraction.script;

import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTarget;
import io.github.kxyr0.echonpcinteraction.core.command.DeterministicInteractionIds;
import io.github.kxyr0.echonpcinteraction.forge.api.service.EchoNpcInteractionServiceBridge;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class NpcInteractionScriptFacade {
    private static final long SCRIPT_CREATED_TICK = 0L;

    public Optional<NpcInteractionResult> interact(MinecraftServer server, String ownerExecution,
            Map<String, String> payload) {
        var command = NpcInteractionScriptPayloads.interact(payload);
        var lookup = EchoNpcInteractionServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lookup.service().orElseThrow().interact(request(ownerExecution, command,
                SCRIPT_CREATED_TICK)));
    }

    public Optional<NpcInteractionResult> stop(MinecraftServer server, String ownerExecution,
            Map<String, String> payload) {
        var lookup = EchoNpcInteractionServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lookup.service().orElseThrow().stop(NpcInteractionScriptPayloads.actor(payload),
                DeterministicInteractionIds.owner(ownerExecution)));
    }

    public Optional<NpcInteractionResult> standUp(MinecraftServer server, String ownerExecution,
            Map<String, String> payload) {
        var lookup = EchoNpcInteractionServiceBridge.lookup(server);
        if (lookup.service().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lookup.service().orElseThrow().standUp(NpcInteractionScriptPayloads.actor(payload),
                DeterministicInteractionIds.owner(ownerExecution)));
    }

    public Optional<NpcInteractionResult> result(MinecraftServer server,
            io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId commandId) {
        return EchoNpcInteractionServiceBridge.lookup(server).queryService()
                .flatMap(query -> query.command(commandId));
    }

    public boolean isTerminal(NpcInteractionResult result) {
        return result.status().terminal() || result.status() == NpcInteractionStatus.COMPLETED;
    }

    private NpcInteractionRequest request(String ownerExecution, NpcInteractionScriptPayloads.InteractionCommand command,
            long tick) {
        return new NpcInteractionRequest(DeterministicInteractionIds.command(ownerExecution, command.actorId(),
                command.position()), DeterministicInteractionIds.execution(ownerExecution),
                DeterministicInteractionIds.owner(ownerExecution), command.actorId(),
                new NpcInteractionTarget(command.dimension(), command.position()), command.face(), command.hand(),
                command.approach(), tick);
    }
}
