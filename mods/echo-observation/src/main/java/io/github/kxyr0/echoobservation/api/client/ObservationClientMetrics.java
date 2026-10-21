package io.github.kxyr0.echoobservation.api.client;

public record ObservationClientMetrics(
        int totalObservations,
        int activeObservations,
        int nearbyCandidates,
        int projectedCandidates,
        int visibleRings,
        int occlusionRaycasts,
        int packetsLastSecond,
        double targetScreenX,
        double targetScreenY) {
}
