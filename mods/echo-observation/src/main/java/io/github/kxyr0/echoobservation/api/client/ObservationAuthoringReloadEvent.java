package io.github.kxyr0.echoobservation.api.client;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class ObservationAuthoringReloadEvent extends Event {
    private final ObservationId observationId;
    private final boolean successful;
    private final long catalogRevision;
    private final String message;

    public ObservationAuthoringReloadEvent(ObservationId observationId, boolean successful,
            long catalogRevision, String message) {
        this.observationId = Objects.requireNonNull(observationId, "observationId");
        this.successful = successful;
        this.catalogRevision = catalogRevision;
        this.message = Objects.requireNonNull(message, "message");
    }

    public ObservationId observationId() { return observationId; }
    public boolean successful() { return successful; }
    public long catalogRevision() { return catalogRevision; }
    public String message() { return message; }
}
