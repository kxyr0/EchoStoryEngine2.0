package io.github.kxyr0.echoseating.api.event;

import java.util.Objects;

public record PlayerStoodUpEvent(PlayerSeatEvent seatEvent) {
    public PlayerStoodUpEvent {
        seatEvent = Objects.requireNonNull(seatEvent, "seatEvent");
        if (seatEvent.kind() != PlayerSeatingEventKind.STOOD_UP) {
            throw new IllegalArgumentException("PlayerStoodUpEvent requires STOOD_UP kind.");
        }
    }
}
