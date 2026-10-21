package io.github.kxyr0.echoevents.api.catalog;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record EventCatalogDescriptor(EventKind kind, Scope scope, List<String> payloadFields,
        List<String> filterFields) {
    public EventCatalogDescriptor {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(scope, "scope");
        payloadFields = List.copyOf(payloadFields);
        filterFields = List.copyOf(filterFields);
    }
    public static List<EventCatalogDescriptor> catalog() {
        return Arrays.stream(EventKind.values()).map(kind -> {
            var scope = switch (kind) {
                case NPC_DIED -> Scope.WORLD;
                case ANIMATION_MARKER, ANIMATION_COMPLETED, NPC_ARRIVED,
                        TELEPORT_STARTED, TELEPORT_COMMITTED, TELEPORT_COMPLETED, TELEPORT_CANCELLED, TELEPORT_FAILED -> Scope.EXECUTION;
                default -> Scope.PLAYER;
            };
            List<String> payload = switch (kind) {
                case DIALOGUE_CHOICE -> List.of("choice");
                case DIALOGUE_FAILED, SCENE_FAILED -> List.of("failure");
                case NPC_ARRIVED -> List.of("actor", "destination");
                case NPC_DIED -> List.of("actor");
                case ANIMATION_MARKER -> List.of("subject", "playback", "marker");
                case ANIMATION_COMPLETED -> List.of("subject", "playback");
                case OBSERVATION_COMPLETED -> List.of("completion");
                case TELEPORT_STARTED, TELEPORT_COMMITTED, TELEPORT_COMPLETED, TELEPORT_CANCELLED, TELEPORT_FAILED -> List.of("subject");
                default -> List.of("phase");
            };
            List<String> filters = switch (kind) {
                case DIALOGUE_CHOICE -> List.of("resource", "choice");
                case NPC_ARRIVED -> List.of("actor", "destination");
                case ANIMATION_MARKER -> List.of("subject", "resource", "marker");
                case ANIMATION_COMPLETED -> List.of("subject", "resource");
                case TELEPORT_STARTED, TELEPORT_COMMITTED, TELEPORT_COMPLETED, TELEPORT_CANCELLED, TELEPORT_FAILED -> List.of("resource");
                default -> List.of("resource");
            };
            return new EventCatalogDescriptor(kind, scope, payload, filters);
        }).toList();
    }
    public enum Scope { PLAYER, EXECUTION, WORLD }
}
