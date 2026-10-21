package io.github.kxyr0.echotriggerseatingadapter.forge;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeatingTriggerEventProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echotriggerseatingadapter");
    private final SeatingStoryContextInheritance contextInheritance = new SeatingStoryContextInheritance();
    private final SeatingDialogueLauncher dialogueLauncher = new SeatingDialogueLauncher();
    private SeatingSubscriptionIndex index = SeatingSubscriptionIndex.empty();
    private long indexRevision = -1L;

    public void accept(PlayerSeatEvent event, MinecraftServer server) {
        Optional<TriggerServiceBridge> trigger = trigger(server);
        if (trigger.isEmpty()) {
            LOGGER.warn("Echo Trigger unavailable for seating event {}", event.eventId().value());
            return;
        }
        refreshIndex(trigger.orElseThrow());
        List<TriggerStorySubscription> matches = index.matches(event);
        trigger.orElseThrow().events().applyEvent(triggerEvent(event));
        matches.forEach(subscription -> route(subscription, event, server, trigger.orElseThrow()));
    }

    private void route(
            TriggerStorySubscription subscription,
            PlayerSeatEvent event,
            MinecraftServer server,
            TriggerServiceBridge trigger) {
        TriggerOwnerId owner = owner(event);
        TriggerCommandId commandId = command(event, subscription);
        var result = subscription.fireOnlyOnce()
                ? trigger.runtime().start(subscription.triggerId(), owner, commandId)
                : trigger.runtime().reset(subscription.triggerId(), owner, commandId);
        if (result.code() == TriggerResultCode.EXISTING) {
            return;
        }
        Optional<TriggerSnapshot> snapshot = result.snapshot();
        if (snapshot.isEmpty()) {
            LOGGER.warn("Seating trigger did not start: {} {}", subscription.triggerId(), result.message());
            return;
        }
        if (subscription.startsDialogue()) {
            boolean started = dialogueLauncher.start(subscription, event, server);
            if (!started) {
                rollbackDialogueTrigger(subscription, event, trigger, snapshot.orElseThrow());
            }
        } else {
            startStory(subscription, event, server, snapshot.orElseThrow());
        }
    }

    private void rollbackDialogueTrigger(
            TriggerStorySubscription subscription,
            PlayerSeatEvent event,
            TriggerServiceBridge trigger,
            TriggerSnapshot snapshot) {
        var rollback = trigger.runtime().stop(snapshot.instanceId(), rollbackCommand(event, subscription));
        if (rollback.snapshot().isEmpty()) {
            LOGGER.warn("Seating trigger rollback failed: trigger={} event={} result={} message={}",
                    subscription.triggerId(), event.eventId().value(), rollback.code(), rollback.message());
        }
    }

    private void startStory(
            TriggerStorySubscription subscription,
            PlayerSeatEvent event,
            MinecraftServer server,
            TriggerSnapshot triggerSnapshot) {
        EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
        if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
            LOGGER.warn("Story execution unavailable for seating event {}: {}", event.eventId().value(),
                    lookup.message());
            return;
        }
        ExecutionId executionId = storyExecution(event, subscription);
        contextInheritance.install(executionId, event, subscription, triggerSnapshot.executionId().value());
        lookup.starter().orElseThrow().start(StoryId.of(subscription.storyId()), executionId);
    }

    private void refreshIndex(TriggerServiceBridge trigger) {
        long revision = trigger.subscriptions().catalogRevision();
        if (revision != indexRevision) {
            index = SeatingSubscriptionIndex.from(trigger.subscriptions().storySubscriptions());
            indexRevision = revision;
        }
    }

    private Optional<TriggerServiceBridge> trigger(MinecraftServer server) {
        TriggerServiceLookupEvent lookup = new TriggerServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge();
    }

    private TriggerEvent triggerEvent(PlayerSeatEvent event) {
        return TriggerEvent.vanilla(owner(event), event.serverTick(), "Player." + event.kind().name(), Map.of(
                "provider", "echo_seating:events",
                "event", event.kind().name(),
                "zone", event.zoneId().value(),
                "slot", event.slotId().map(slot -> slot.value()).orElse(""),
                "seatEvent", event.eventId().value(),
                "player", event.player().uuid().toString()));
    }

    private TriggerOwnerId owner(PlayerSeatEvent event) {
        return new TriggerOwnerId("player:" + event.player().uuid());
    }

    private TriggerCommandId command(PlayerSeatEvent event, TriggerStorySubscription subscription) {
        return new TriggerCommandId("seating:" + event.eventId().value() + ":" + subscription.subscriptionId());
    }

    private TriggerCommandId rollbackCommand(PlayerSeatEvent event, TriggerStorySubscription subscription) {
        return new TriggerCommandId("seating:rollback:" + event.eventId().value() + ":"
                + subscription.subscriptionId());
    }

    private ExecutionId storyExecution(PlayerSeatEvent event, TriggerStorySubscription subscription) {
        String kind = event.kind().name().toLowerCase(Locale.ROOT);
        String eventId = event.eventId().value().replace(':', '_');
        String subscriptionId = subscription.subscriptionId().value().replace(':', '_');
        return ExecutionId.of("echo_seating:story_claim/" + eventId + "/" + subscriptionId + "/" + kind);
    }
}
