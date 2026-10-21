package io.github.kxyr0.echoobservation.forge.network;

import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ObservationPackets {
    private ObservationPackets() {
    }

    public enum FocusKind { START, HEARTBEAT, STOP }
    public enum FocusStatus { ACCEPTED, REJECTED, STOPPED, COMPLETED }

    public record ClientboundSnapshot(
            long sequence,
            long catalogRevision,
            List<ObservationResourceDefinition> resources,
            Set<String> activeResources,
            Set<String> disabledObservations,
            Set<String> completedObservations) {
        public ClientboundSnapshot {
            resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
            activeResources = Set.copyOf(Objects.requireNonNull(activeResources, "activeResources"));
            disabledObservations = Set.copyOf(Objects.requireNonNull(disabledObservations, "disabledObservations"));
            completedObservations = Set.copyOf(Objects.requireNonNull(completedObservations, "completedObservations"));
        }
    }

    public record ServerboundFocus(FocusKind kind, String observationId, long definitionRevision) { }
    public record ClientboundFocus(FocusStatus status, String observationId, int requiredTicks, String message) { }
    public record ClientboundText(String observationId, String text, long expiresAtMillis) { }
    public record ServerboundReload(String observationId) { }
    public record ClientboundReload(String observationId, boolean successful, long catalogRevision, String message) { }
}
