package io.github.kxyr0.echotriggerplayersleepadapter.forge;

import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.forge.api.context.PlayerSleepStoryContextBridge;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SleepStoryContextInheritance {
    public void install(
            ExecutionId storyExecutionId,
            PlayerSleepEventSnapshot event,
            TriggerStorySubscription subscription,
            String triggerExecutionId) {
        PlayerSleepStoryContextBridge.install(storyExecutionId.asString(),
                context(storyExecutionId, event, subscription, triggerExecutionId));
    }

    private Map<String, String> context(
            ExecutionId storyExecutionId,
            PlayerSleepEventSnapshot event,
            TriggerStorySubscription subscription,
            String triggerExecutionId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", event.player().uuid().toString());
        values.put("scenario", event.scenarioId().value());
        values.put("owner", "player:" + event.player().uuid());
        values.put("command", "sleep-story:" + event.eventId().value());
        values.put("sleep.player", event.player().uuid().toString());
        values.put("sleep.session", event.session().sessionId().value());
        values.put("sleep.scenario", event.scenarioId().value());
        values.put("sleep.count", Long.toString(event.sleepCount()));
        values.put("sleep.originDimension", event.originDimension().value());
        values.put("sleep.bedPosition", event.bedPosition().compact());
        values.put("sleep.bedBlock", event.bedBlock().value());
        values.put("sleep.bedProvider", event.bedProviderId());
        values.put("sleep.revision", Long.toString(event.session().revision().value()));
        values.put("trigger.subscription", subscription.subscriptionId().value());
        values.put("trigger.execution", triggerExecutionId);
        values.put("story.execution", storyExecutionId.asString());
        return Map.copyOf(values);
    }
}
