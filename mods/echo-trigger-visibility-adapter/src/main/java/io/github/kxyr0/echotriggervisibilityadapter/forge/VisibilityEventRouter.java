package io.github.kxyr0.echotriggervisibilityadapter.forge;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.forge.api.context.VisibilityStoryContextBridge;
import io.github.kxyr0.echovisibility.forge.api.lifecycle.VisibilityLifecycleEvent;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceBridge;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VisibilityEventRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "io.github.kxyr0.echotriggervisibilityadapter");
    private final VisibilityContextPayload contexts = new VisibilityContextPayload();
    private final VisibilityConcealmentMonitor concealments = new VisibilityConcealmentMonitor();
    private VisibilitySubscriptionIndex index = VisibilitySubscriptionIndex.empty();
    private long indexRevision = -1L;

    public void accept(VisibilityLifecycleEvent event) {
        MinecraftServer server = event.player().server;
        Optional<TriggerServiceBridge> trigger = trigger(server);
        Optional<VisibilityServiceBridge> visibility = visibility(server);
        EchoStoryExecutionLookupResult story = EchoStoryExecutionBridge.lookup(server);
        if (trigger.isEmpty() || visibility.isEmpty()
                || story.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
            rejectLease(event, visibility);
            diagnostic(VisibilityAdapterDiagnostic.TRIGGER_VISIBILITY_PROVIDER_UNAVAILABLE,
                    "Trigger, Visibility, or Story provider is unavailable.");
            return;
        }
        refresh(trigger.orElseThrow());
        Optional<String> eventKind = eventKind(event);
        List<TriggerStorySubscription> matches = eventKind.map(kind ->
                index.matches(kind, event.report().watchId().value())).orElseGet(List::of);
        if (matches.isEmpty()) {
            return;
        }
        if (matches.size() != 1) {
            rejectLease(event, visibility);
            diagnostic(VisibilityAdapterDiagnostic.TRIGGER_VISIBILITY_SELECTOR_INVALID,
                    "Visibility event matched " + matches.size() + " subscriptions.");
            return;
        }
        route(matches.get(0), event, trigger.orElseThrow(), visibility.orElseThrow(), story);
    }

    public void tick(MinecraftServer server) {
        if (concealments.size() > 0) {
            visibility(server).ifPresent(value -> concealments.tick(server, value));
        }
    }

    private void route(
            TriggerStorySubscription subscription,
            VisibilityLifecycleEvent event,
            TriggerServiceBridge trigger,
            VisibilityServiceBridge visibility,
            EchoStoryExecutionLookupResult story) {
        if (!VisibilityClaimSavedData.get(event.player().server).claim(event.eventId().value())) {
            rejectLease(event, Optional.of(visibility));
            diagnostic(VisibilityAdapterDiagnostic.TRIGGER_VISIBILITY_EXECUTION_DUPLICATE,
                    "Visibility event was already claimed: " + event.eventId().value());
            return;
        }
        event.report().concealmentLeaseId().ifPresent(lease ->
                visibility.service().claimConcealment(event.player(), lease,
                        "trigger:" + subscription.triggerId().value()));
        TriggerOwnerId owner = owner(event);
        Map<String, String> preContext = contexts.create(event, subscription, "pending", "pending");
        trigger.events().applyEvent(TriggerEvent.vanilla(owner, event.player().server.getTickCount(),
                "Player." + eventKind(event).orElseThrow(), preContext));
        var result = trigger.runtime().start(subscription.triggerId(), owner,
                new TriggerCommandId("visibility-event:" + event.eventId().value()));
        if (result.code() != TriggerResultCode.STARTED || result.snapshot().isEmpty()) {
            VisibilityClaimSavedData.get(event.player().server).terminal(event.eventId().value(), "TRIGGER_FAILED");
            rejectLease(event, Optional.of(visibility));
            diagnostic(VisibilityAdapterDiagnostic.TRIGGER_VISIBILITY_EXECUTION_DUPLICATE,
                    result.message().orElse("Trigger execution did not start."));
            return;
        }
        ExecutionId storyExecution = storyExecution(event);
        Map<String, String> context = contexts.create(event, subscription,
                result.snapshot().orElseThrow().executionId().value(), storyExecution.asString());
        try {
            VisibilityStoryContextBridge.install(storyExecution.asString(), context);
            story.starter().orElseThrow().start(StoryId.of(subscription.storyId()), storyExecution);
            VisibilityClaimSavedData.get(event.player().server).terminal(
                    event.eventId().value(), "STORY_STARTED");
            monitorConcealment(event);
        } catch (RuntimeException exception) {
            VisibilityClaimSavedData.get(event.player().server).terminal(event.eventId().value(), "STORY_FAILED");
            rejectLease(event, Optional.of(visibility));
            diagnostic(VisibilityAdapterDiagnostic.TRIGGER_VISIBILITY_CONTEXT_INHERITANCE_FAILED,
                    exception.getMessage());
        }
    }

    private void monitorConcealment(VisibilityLifecycleEvent event) {
        if (event.watch().definition().target() instanceof VisibilityNpcTarget npc) {
            event.report().concealmentLeaseId().ifPresent(lease -> concealments.track(
                    event.player().getUUID(), lease, npc.npcId(), event.player().server.getTickCount()));
        }
    }

    private void refresh(TriggerServiceBridge trigger) {
        long revision = trigger.subscriptions().catalogRevision();
        if (revision != indexRevision) {
            index = VisibilitySubscriptionIndex.from(trigger.subscriptions().storySubscriptions());
            indexRevision = revision;
        }
    }

    private static Optional<String> eventKind(VisibilityLifecycleEvent event) {
        VisibilityTransitionKind kind = event.report().transition().kind();
        if (kind == VisibilityTransitionKind.LOST_SIGHT) {
            return Optional.of("LOST_SIGHT");
        }
        if (kind == VisibilityTransitionKind.GAINED_SIGHT) {
            return Optional.of("GAINED_SIGHT");
        }
        return event.report().state().geometricallyHidden()
                ? Optional.of("TARGET_IS_OUT_OF_VIEW") : Optional.empty();
    }

    private static TriggerOwnerId owner(VisibilityLifecycleEvent event) {
        return new TriggerOwnerId("visibility:" + event.player().getUUID() + ":"
                + event.report().watchId().value() + ":" + event.report().generation().value());
    }

    private static ExecutionId storyExecution(VisibilityLifecycleEvent event) {
        return ExecutionId.of("echo_visibility:story/" + event.eventId().value().replace(':', '/'));
    }

    private static Optional<TriggerServiceBridge> trigger(MinecraftServer server) {
        TriggerServiceLookupEvent lookup = new TriggerServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge();
    }

    private static Optional<VisibilityServiceBridge> visibility(MinecraftServer server) {
        VisibilityServiceLookupEvent lookup = new VisibilityServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge();
    }

    private static void rejectLease(
            VisibilityLifecycleEvent event,
            Optional<VisibilityServiceBridge> visibility) {
        event.report().concealmentLeaseId().ifPresent(lease -> visibility.ifPresent(value ->
                value.service().rejectConcealment(event.player(), lease)));
    }

    private static void diagnostic(VisibilityAdapterDiagnostic code, String message) {
        LOGGER.warn("{}: {}", code, message);
    }
}
