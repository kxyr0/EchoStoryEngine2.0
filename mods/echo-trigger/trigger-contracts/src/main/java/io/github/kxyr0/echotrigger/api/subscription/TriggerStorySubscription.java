package io.github.kxyr0.echotrigger.api.subscription;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.Objects;

public record TriggerStorySubscription(
        TriggerSubscriptionId subscriptionId,
        TriggerId triggerId,
        TriggerEventSelector selector,
        TriggerSubscriptionTargetKind targetKind,
        String targetId,
        boolean fireOnlyOnce) {
    public TriggerStorySubscription(
            TriggerSubscriptionId subscriptionId,
            TriggerId triggerId,
            TriggerEventSelector selector,
            String storyId,
            boolean fireOnlyOnce) {
        this(subscriptionId, triggerId, selector, TriggerSubscriptionTargetKind.STORY, storyId, fireOnlyOnce);
    }

    public TriggerStorySubscription {
        subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId");
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        selector = Objects.requireNonNull(selector, "selector");
        targetKind = Objects.requireNonNull(targetKind, "targetKind");
        targetId = requireId(targetId, "targetId");
    }

    public boolean startsStory() {
        return targetKind == TriggerSubscriptionTargetKind.STORY;
    }

    public boolean startsDialogue() {
        return targetKind == TriggerSubscriptionTargetKind.DIALOGUE;
    }

    public String storyId() {
        if (!startsStory()) {
            throw new IllegalStateException("Trigger subscription target is not a Story.");
        }
        return targetId;
    }

    public String dialogueId() {
        if (!startsDialogue()) {
            throw new IllegalStateException("Trigger subscription target is not a Dialogue.");
        }
        return targetId;
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespace:path.");
        }
        return checked;
    }
}
