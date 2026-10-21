package io.github.kxyr0.echotriggervisibilityadapter.forge;

import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.forge.api.lifecycle.VisibilityLifecycleEvent;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VisibilityContextPayload {
    public Map<String, String> create(
            VisibilityLifecycleEvent event,
            TriggerStorySubscription subscription,
            String triggerExecution,
            String storyExecution) {
        var report = event.report();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", event.player().getUUID().toString());
        values.put("visibility.watch", report.watchId().value());
        values.put("visibility.generation", Long.toString(report.generation().value()));
        values.put("visibility.event", event.eventId().value());
        values.put("visibility.state", report.state().name());
        values.put("visibility.reason", report.transition().kind().name());
        values.put("visibility.lastVisibleFrame", Long.toString(report.transition().previousStableFrame()));
        values.put("visibility.lostFrame", Long.toString(report.renderedFrame()));
        values.put("visibility.targetRevision", Long.toString(report.targetRevision().value()));
        values.put("visibility.camera.source", report.camera().source());
        values.put("visibility.camera.position", report.camera().position().toString());
        values.put("visibility.camera.forward", report.camera().forward().toString());
        values.put("visibility.camera.fov", Double.toString(report.camera().dynamicFovDegrees()));
        values.put("visibility.concealmentLease",
                report.concealmentLeaseId().map(value -> value.value()).orElse(""));
        if (event.watch().definition().target() instanceof VisibilityNpcTarget npc) {
            values.put("visibility.targetType", "NPC");
            values.put("visibility.npc", npc.npcId());
        } else if (event.watch().definition().target() instanceof VisibilityBoxTarget box) {
            values.put("visibility.targetType", "BOX");
            values.put("visibility.box", box.dimension().value() + ":" + box.bounds());
        }
        values.put("trigger.subscription", subscription.subscriptionId().value());
        values.put("trigger.execution", triggerExecution);
        values.put("story.execution", storyExecution);
        return Map.copyOf(values);
    }
}
