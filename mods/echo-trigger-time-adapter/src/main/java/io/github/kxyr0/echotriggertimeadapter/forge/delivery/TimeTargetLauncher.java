package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import io.github.kxyr0.echotime.forge.api.context.EchoTimeStoryContextBridge;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionTargetKind;
import io.github.kxyr0.echotriggertimeadapter.forge.TimeAdapterServices;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeTriggerDelivery;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

final class TimeTargetLauncher {
    boolean launch(TimeTriggerDelivery delivery, MinecraftServer server) {
        return delivery.targetKind() == TriggerSubscriptionTargetKind.STORY
                ? story(delivery, server) : dialogue(delivery, server);
    }

    private boolean story(TimeTriggerDelivery delivery, MinecraftServer server) {
        EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
        if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) return false;
        ExecutionId execution = ExecutionId.of("echo_time:trigger/" + delivery.deliveryId());
        EchoTimeStoryContextBridge.install(execution.asString(), context(delivery));
        lookup.starter().orElseThrow().start(StoryId.of(delivery.targetId()), execution);
        return true;
    }

    private boolean dialogue(TimeTriggerDelivery delivery, MinecraftServer server) {
        var service = TimeAdapterServices.dialogue(server);
        if (service.isEmpty() || delivery.audienceId().isEmpty()) return false;
        var result = service.orElseThrow().start(new DialogueStartRequest(
                new DialogueSessionId("time-trigger/" + delivery.deliveryId()),
                DialogueId.parse(delivery.targetId()), new AudienceId(delivery.audienceId().orElseThrow())));
        return switch (result.status()) {
            case ACCEPTED, STARTED, ALREADY_ACTIVE, WAITING, ALREADY_TERMINAL -> true;
            default -> false;
        };
    }

    private Map<String, String> context(TimeTriggerDelivery value) {
        return Map.of("time.occurrenceId", value.occurrenceId(), "time.subscriptionId", value.subscriptionId(),
                "time.triggerId", value.triggerId(), "time.day", Long.toString(value.day()),
                "time.time", value.time(), "time.intervalStart", Long.toString(value.intervalStart()),
                "time.intervalEnd", Long.toString(value.intervalEnd()), "time.crossingReason", value.crossingReason());
    }
}
