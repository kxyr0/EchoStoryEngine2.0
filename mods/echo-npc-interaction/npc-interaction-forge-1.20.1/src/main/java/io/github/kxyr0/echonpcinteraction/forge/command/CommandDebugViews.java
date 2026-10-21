package io.github.kxyr0.echonpcinteraction.forge.command;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import io.github.kxyr0.echonpcinteraction.forge.inspect.ServerTargetInspector;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;

final class CommandDebugViews {
    private CommandDebugViews() {
    }

    static String approaches(MinecraftServer server, NpcInteractionRequest request,
            NpcInteractionProviderRegistry providers, Supplier<List<NpcInteractionExecutionPort>> ports) {
        var target = new ServerTargetInspector().inspect(server, request.target());
        if (target.isEmpty() || providers.providers().isEmpty() || ports.get().isEmpty()) {
            return "No approaches.";
        }
        var provider = providers.providers().get(0);
        var plan = provider.plan(request, target.orElseThrow());
        return String.join("\n", ports.get().get(0).approachDiagnostics(server, request, plan));
    }

    static String ride(MinecraftServer server, ActorId npcId, NpcSeatOccupancyRegistry occupancies) {
        var body = EchoNpcNavigationBodyBridge.lookup(server, npcId).body();
        String vehicle = body.flatMap(value -> java.util.Optional.ofNullable(value.mob().getVehicle()))
                .map(value -> value.getType().toString() + " " + value.getUUID()).orElse("none");
        String occupancy = occupancies.byNpc(npcId).map(Object::toString).orElse("none");
        return "vehicle=" + vehicle + "\noccupancy=" + occupancy;
    }

    static String seats(NpcSeatOccupancyRegistry occupancies) {
        if (occupancies.all().isEmpty()) {
            return "No seat occupancies.";
        }
        return occupancies.all().stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    static String seat(String dimensionId, BlockPosition position, NpcSeatOccupancyRegistry occupancies) {
        return occupancies.byTarget(dimensionId, position).map(Object::toString).orElse("Seat occupancy not found.");
    }

    static String proxyStatus() {
        return "proxy=EchoNpcUseProxy transient=true visible=false persistentInventory=false";
    }
}
