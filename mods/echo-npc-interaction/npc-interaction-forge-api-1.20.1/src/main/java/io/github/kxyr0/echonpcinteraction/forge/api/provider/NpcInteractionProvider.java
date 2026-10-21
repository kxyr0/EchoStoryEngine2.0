package io.github.kxyr0.echonpcinteraction.forge.api.provider;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.target.NpcInteractionTargetView;
import net.minecraft.server.MinecraftServer;

public interface NpcInteractionProvider {
    NpcInteractionProviderId id();

    NpcInteractionProviderSelection inspect(NpcInteractionTargetView target);

    NpcInteractionPlan plan(NpcInteractionRequest request, NpcInteractionTargetView target);

    NpcInteractionResult execute(
            MinecraftServer server,
            NpcInteractionExecutionPort port,
            NpcInteractionRequest request,
            NpcInteractionPlan plan);

    NpcInteractionResult verify(
            MinecraftServer server,
            NpcInteractionExecutionPort port,
            NpcInteractionRequest request,
            NpcInteractionPlan plan);
}
