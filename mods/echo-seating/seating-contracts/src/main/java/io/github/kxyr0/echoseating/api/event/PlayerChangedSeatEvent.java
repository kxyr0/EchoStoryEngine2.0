package io.github.kxyr0.echoseating.api.event;

import java.util.Objects;

public record PlayerChangedSeatEvent(PlayerSeatEvent seatEvent) {
    public PlayerChangedSeatEvent {
        seatEvent = Objects.requireNonNull(seatEvent, "seatEvent");
        if (seatEvent.kind() != PlayerSeatingEventKind.CHANGED_SEAT) {
            throw new IllegalArgumentException("PlayerChangedSeatEvent requires CHANGED_SEAT kind.");
        }
    }
}
