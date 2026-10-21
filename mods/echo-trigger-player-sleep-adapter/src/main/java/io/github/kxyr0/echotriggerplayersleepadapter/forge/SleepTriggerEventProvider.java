package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventKind;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceLookupEvent;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecycleEvent;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SleepTriggerEventProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echotriggerplayersleepadapter");
    private final SleepSubscriptionConflictValidator validator = new SleepSubscriptionConflictValidator();
    private final SleepStoryContextInheritance contextInheritance = new SleepStoryContextInheritance();
    private final SleepTriggerRecovery recovery = new SleepTriggerRecovery();
    private SleepSubscriptionIndex index = SleepSubscriptionIndex.empty();
    private long indexRevision = -1L;

    public void prepare(PlayerSleepInBedEvent event, MinecraftServer server) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<PlayerSleepServiceBridge> sleep = sleep(server);
        Optional<TriggerServiceBridge> trigger = trigger(server);
        if (sleep.isEmpty() || trigger.isEmpty()) {
            return;
        }
        PlayerRef playerRef = new PlayerRef(player.getUUID());
        if (sleep.orElseThrow().arms().isArmed(playerRef)) {
            return;
        }
        refreshIndex(trigger.orElseThrow());
        long nextSleepCount = sleep.orElseThrow().query().sleepCount(playerRef) + 1L;
        List<TriggerStorySubscription> indexedCandidates = index.armCandidates(nextSleepCount);
        List<TriggerStorySubscription> candidates = indexedCandidates.stream()
                .filter(subscription -> sleep.orElseThrow().catalog().contains(
                        new PlayerSleepScenarioId(subscription.selector().scenarioId())))
                .toList();
        if (candidates.size() != 1) {
            return;
        }
        TriggerStorySubscription subscription = candidates.get(0);
        PlayerSleepScenarioId scenario = new PlayerSleepScenarioId(subscription.selector().scenarioId());
        sleep.orElseThrow().arms().arm(playerRef, scenario,
                new PlayerSleepOwnerId("trigger:" + subscription.triggerId().value()),
                new PlayerSleepCommandId("trigger-prearm:" + player.getUUID()
                        + ":" + nextSleepCount + ":" + subscription.subscriptionId().value()));
        LOGGER.info("Echo Player Sleep prearmed: nextSleepCount={}, trigger={}, scenario={}", nextSleepCount,
                subscription.triggerId().value(), scenario.value());
    }

    public void accept(PlayerSleepLifecycleEvent event, MinecraftServer server) {
        event.sleepEvent().filter(sleepEvent -> sleepEvent.kind() == PlayerSleepEventKind.CONFIRMED)
                .ifPresent(sleepEvent -> route(sleepEvent, server, true));
    }

    public void tick(MinecraftServer server) {
        for (SleepTriggerRecovery.Pending pending : recovery.pending()) {
            RouteStatus status = route(pending.event(), server, false);
            if (status != RouteStatus.WAITING) {
                recovery.remove(pending.event().eventId());
            } else if (recovery.expired(pending, server.getTickCount())) {
                sleep(server).ifPresent(bridge -> fail(bridge, pending.event(),
                        PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_TIMEOUT,
                        "Sleep Story claim timed out after 20 ticks."));
                recovery.remove(pending.event().eventId());
            }
        }
    }

    private RouteStatus route(PlayerSleepEventSnapshot event, MinecraftServer server, boolean allowPending) {
        Optional<PlayerSleepServiceBridge> sleep = sleep(server);
        Optional<TriggerServiceBridge> trigger = trigger(server);
        if (sleep.isEmpty() || trigger.isEmpty()) {
            return RouteStatus.WAITING;
        }
        refreshIndex(trigger.orElseThrow());
        var validation = validator.validate(index.matches(event));
        if (validation.status() == SleepSubscriptionConflictValidator.Status.MISSING) {
            if (allowPending) {
                recovery.await(event, server.getTickCount());
            }
            return RouteStatus.WAITING;
        }
        if (validation.status() == SleepSubscriptionConflictValidator.Status.CONFLICT) {
            fail(sleep.orElseThrow(), event, PlayerSleepFailure.PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT,
                    "Multiple claiming sleep subscriptions matched: " + validation.matches());
            return RouteStatus.FAILED;
        }
        return claim(validation.subscription().orElseThrow(), event, server,
                sleep.orElseThrow(), trigger.orElseThrow());
    }

    private RouteStatus claim(
            TriggerStorySubscription subscription,
            PlayerSleepEventSnapshot event,
            MinecraftServer server,
            PlayerSleepServiceBridge sleep,
            TriggerServiceBridge trigger) {
        TriggerOwnerId owner = owner(event);
        trigger.events().applyEvent(triggerEvent(event, owner));
        var triggerResult = trigger.runtime().start(subscription.triggerId(), owner, triggerCommand(event));
        if (triggerResult.code() == TriggerResultCode.EXISTING) {
            fail(sleep, event, PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_CONFLICT,
                    "Trigger subscription was already claimed for this owner.");
            return RouteStatus.FAILED;
        }
        Optional<TriggerSnapshot> snapshot = triggerResult.snapshot();
        if (snapshot.isEmpty()) {
            fail(sleep, event, PlayerSleepFailure.TRIGGER_SLEEP_PROVIDER_UNAVAILABLE,
                    triggerResult.message().orElse("Trigger subscription did not start."));
            return RouteStatus.FAILED;
        }
        return startStory(subscription, event, server, sleep, snapshot.orElseThrow());
    }

    private RouteStatus startStory(
            TriggerStorySubscription subscription,
            PlayerSleepEventSnapshot event,
            MinecraftServer server,
            PlayerSleepServiceBridge sleep,
            TriggerSnapshot triggerSnapshot) {
        EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
        if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
            fail(sleep, event, PlayerSleepFailure.PLAYER_SLEEP_STORY_START_FAILED, lookup.message());
            return RouteStatus.FAILED;
        }
        ExecutionId executionId = storyExecution(event);
        contextInheritance.install(executionId, event, subscription, triggerSnapshot.executionId().value());
        lookup.starter().orElseThrow().start(StoryId.of(subscription.storyId()), executionId);
        var result = sleep.claims().claim(event.session().sessionId(), subscription.subscriptionId().value(),
                triggerSnapshot.executionId().value(), executionId.asString(), new PlayerSleepOwnerId(owner(event).value()),
                new PlayerSleepCommandId("sleep-claim:" + event.eventId().value()), event.serverTick());
        return result.diagnostics().isEmpty() ? RouteStatus.CLAIMED : RouteStatus.FAILED;
    }

    private void refreshIndex(TriggerServiceBridge trigger) {
        long revision = trigger.subscriptions().catalogRevision();
        if (revision != indexRevision) {
            index = SleepSubscriptionIndex.from(trigger.subscriptions().storySubscriptions());
            indexRevision = revision;
        }
    }

    private Optional<PlayerSleepServiceBridge> sleep(MinecraftServer server) {
        PlayerSleepServiceLookupEvent lookup = new PlayerSleepServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge();
    }

    private Optional<TriggerServiceBridge> trigger(MinecraftServer server) {
        TriggerServiceLookupEvent lookup = new TriggerServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.bridge();
    }

    private void fail(
            PlayerSleepServiceBridge sleep,
            PlayerSleepEventSnapshot event,
            PlayerSleepFailure code,
            String message) {
        sleep.claims().failUnclaimed(event.session().sessionId(), code, message);
    }

    private TriggerEvent triggerEvent(PlayerSleepEventSnapshot event, TriggerOwnerId owner) {
        return TriggerEvent.vanilla(owner, event.serverTick(), "PlayerSleep." + event.kind().name(), Map.of(
                "provider", "echo_player_sleep:events",
                "event", event.kind().name(),
                "scenario", event.scenarioId().value(),
                "session", event.session().sessionId().value(),
                "sleepCount", Long.toString(event.sleepCount()),
                "sleep.count", Long.toString(event.sleepCount())));
    }

    private TriggerOwnerId owner(PlayerSleepEventSnapshot event) {
        return new TriggerOwnerId("player:" + event.player().uuid());
    }

    private TriggerCommandId triggerCommand(PlayerSleepEventSnapshot event) {
        return new TriggerCommandId("sleep-event:" + event.eventId().value());
    }

    private ExecutionId storyExecution(PlayerSleepEventSnapshot event) {
        String status = event.kind().name().toLowerCase(Locale.ROOT);
        String session = event.session().sessionId().value().replace('-', '_');
        return ExecutionId.of("echo_player_sleep:story_claim/" + session + "/" + status);
    }

    private enum RouteStatus { CLAIMED, FAILED, WAITING }
}
