package io.github.kxyr0.echonpcinteraction.forge.api.execution;

import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public interface NpcInteractionExecutionPort {
    String portId();

    Optional<NpcInteractionApproach> selectApproach(
            MinecraftServer server,
            NpcInteractionRequest request,
            NpcInteractionPlan plan);

    NavigationDispatch startNavigation(
            MinecraftServer server,
            NpcInteractionRequest request,
            NpcInteractionApproach approach);

    Optional<NavigationObservation> navigation(MinecraftServer server, NavigationMoveId moveId);

    NavigationDispatch cancelNavigation(MinecraftServer server, NavigationMoveId moveId);

    default List<String> approachDiagnostics(
            MinecraftServer server,
            NpcInteractionRequest request,
            NpcInteractionPlan plan) {
        return plan.approaches().stream().map(Object::toString).toList();
    }
}
