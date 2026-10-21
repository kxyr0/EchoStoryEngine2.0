package io.github.kxyr0.echoseating.forge.api.lifecycle;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerSeatingLifecycleEvent extends Event {
    private final PlayerSeatEvent seatEvent;

    public PlayerSeatingLifecycleEvent(PlayerSeatEvent seatEvent) {
        this.seatEvent = Objects.requireNonNull(seatEvent, "seatEvent");
    }

    public PlayerSeatEvent seatEvent() {
        return seatEvent;
    }
}
