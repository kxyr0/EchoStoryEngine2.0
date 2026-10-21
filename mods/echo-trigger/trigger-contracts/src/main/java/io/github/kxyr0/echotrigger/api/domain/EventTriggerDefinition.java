package io.github.kxyr0.echotrigger.api.domain;

import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import java.util.List;
import java.util.Objects;

public record EventTriggerDefinition(EventSubscription subscription, Location matcherSource, List<Action> actions) {
    public EventTriggerDefinition {
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(matcherSource, "matcherSource");
        actions = List.copyOf(actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("Event trigger requires actions");
        }
    }
    public record Location(String sourceId, int line, int column) {
        public Location {
            Objects.requireNonNull(sourceId, "sourceId");
            if (line < 1 || column < 1) {
                throw new IllegalArgumentException("Invalid event source location");
            }
        }
    }
    public record Action(Kind kind, ResourceId target, Location source) {
        public Action {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(source, "source");
        }
    }
    public enum Kind {
        SCENE_PLAY, STORY_START, DIALOGUE_PLAY, OBSERVATION_ACTIVATE, OBSERVATION_ENABLE
    }
}
