package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.core.provider.NpcInteractionProviderSelector;
import io.github.kxyr0.echonpcinteraction.core.validate.NpcInteractionPlanValidator;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProvider;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import io.github.kxyr0.echonpcinteraction.forge.inspect.ServerTargetInspector;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

final class InteractionStartFlow {
    private final MinecraftServer server;
    private final NpcInteractionProviderRegistry providers;
    private final Supplier<List<NpcInteractionExecutionPort>> ports;
    private final ServerTargetInspector inspector = new ServerTargetInspector();

    InteractionStartFlow(MinecraftServer server, NpcInteractionProviderRegistry providers,
            Supplier<List<NpcInteractionExecutionPort>> ports) {
        this.server = server;
        this.providers = providers;
        this.ports = ports;
    }

    StartOutcome start(NpcInteractionRequest request) {
        Optional<NpcInteractionPlan> plan = plan(request);
        if (plan.isEmpty()) {
            return StartOutcome.failed(fail(request, NpcInteractionOutcome.NO_PROVIDER,
                    NpcInteractionFailure.NPC_INTERACTION_PROVIDER_NOT_FOUND, "Provider not found."));
        }
        Optional<NpcInteractionExecutionPort> port = ports.get().stream().findFirst();
        if (port.isEmpty()) {
            return StartOutcome.failed(fail(request, NpcInteractionOutcome.NAVIGATION_FAILED,
                    NpcInteractionFailure.NPC_INTERACTION_CONTEXT_MISSING, "Navigation adapter is unavailable."));
        }
        var selected = port.orElseThrow().selectApproach(server, request, plan.orElseThrow());
        if (selected.isEmpty()) {
            return StartOutcome.failed(fail(request, NpcInteractionOutcome.NO_APPROACH,
                    NpcInteractionFailure.NPC_INTERACTION_NO_APPROACH, "No reachable approach."));
        }
        var dispatch = port.orElseThrow().startNavigation(server, request, selected.orElseThrow());
        if (dispatch.moveId().isEmpty()) {
            return StartOutcome.failed(fail(request, NpcInteractionOutcome.NAVIGATION_FAILED,
                    NpcInteractionFailure.NPC_INTERACTION_NAVIGATION_FAILED, dispatch.message()));
        }
        NpcInteractionPlan chosen = new NpcInteractionPlan(plan.orElseThrow().providerId(),
                plan.orElseThrow().targetSnapshot(), List.of(selected.orElseThrow()),
                plan.orElseThrow().executionPolicy(), plan.orElseThrow().verificationPolicy(),
                plan.orElseThrow().planRevision());
        InteractionRecord record = new InteractionRecord(request, NpcInteractionStatus.WAITING_FOR_NAVIGATION,
                chosen, dispatch.moveId(), server.overworld().getGameTime());
        return StartOutcome.accepted(record,
                NpcInteractionResult.running(request.commandId(), NpcInteractionStatus.WAITING_FOR_NAVIGATION,
                        "Interaction is waiting for navigation."));
    }

    private Optional<NpcInteractionPlan> plan(NpcInteractionRequest request) {
        var target = inspector.inspect(server, request.target());
        if (target.isEmpty()) {
            return Optional.empty();
        }
        Optional<NpcInteractionProvider> provider = providerFor(target.orElseThrow());
        if (provider.isEmpty()) {
            return Optional.empty();
        }
        NpcInteractionPlan plan = provider.orElseThrow().plan(request, target.orElseThrow());
        return new NpcInteractionPlanValidator().validate(plan).isPresent() ? Optional.empty() : Optional.of(plan);
    }

    private Optional<NpcInteractionProvider> providerFor(
            io.github.kxyr0.echonpcinteraction.forge.api.target.NpcInteractionTargetView target) {
        NpcInteractionProviderSelector selector = new NpcInteractionProviderSelector();
        var candidates = providers.providers().stream().map(provider -> {
            var selection = provider.inspect(target);
            return new NpcInteractionProviderSelector.Candidate(provider.id(), selection.priority(),
                    selection.specificity(), selection.status().name().equals("SUPPORTED"));
        }).toList();
        return selector.select(candidates).providerId().flatMap(providers::find);
    }

    private NpcInteractionResult fail(NpcInteractionRequest request, NpcInteractionOutcome outcome,
            NpcInteractionFailure failure, String message) {
        return NpcInteractionResult.failed(request.commandId(), outcome, failure, message);
    }
}
