package io.github.kxyr0.echoseating.api.event;

import java.util.Objects;

public record PlayerSeatedEvent(PlayerSeatEvent seatEvent) {
    public PlayerSeatedEvent {
        seatEvent = Objects.requireNonNull(seatEvent, "seatEvent");
        if (seatEvent.kind() != PlayerSeatingEventKind.SEATED) {
            throw new IllegalArgumentException("PlayerSeatedEvent requires SEATED kind.");
        }
    }
}
