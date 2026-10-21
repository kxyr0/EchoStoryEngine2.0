package io.github.kxyr0.echoseating.core.lifecycle;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echoseating.api.event.PlayerSeatingEventKind;
import io.github.kxyr0.echoseating.api.id.SeatEventId;
import io.github.kxyr0.echoseating.api.occupancy.SeatOccupancy;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SeatEventLedger {
    private static final int MAX_EVENTS = 2048;
    private final Map<SeatEventId, PlayerSeatEvent> published = new LinkedHashMap<>();

    public Optional<PlayerSeatEvent> publish(
            PlayerSeatingEventKind kind,
            PlayerRef player,
            SeatOccupancy occupancy,
            long ridingGeneration,
            long serverTick) {
        SeatEventId eventId = eventId(kind, player, occupancy, ridingGeneration);
        if (published.containsKey(eventId)) {
            return Optional.empty();
        }
        PlayerSeatEvent event = new PlayerSeatEvent(eventId, kind, player, occupancy.zoneId(),
                occupancy.slotId(), occupancy.anchor(), occupancy.vehicleEntityTypeId(), serverTick,
                occupancy.revision(), fingerprint(kind, occupancy));
        remember(event);
        return Optional.of(event);
    }

    public List<PlayerSeatEvent> events() {
        return List.copyOf(published.values());
    }

    private void remember(PlayerSeatEvent event) {
        published.put(event.eventId(), event);
        while (published.size() > MAX_EVENTS) {
            SeatEventId first = published.keySet().iterator().next();
            published.remove(first);
        }
    }

    private static SeatEventId eventId(
            PlayerSeatingEventKind kind,
            PlayerRef player,
            SeatOccupancy occupancy,
            long generation) {
        String key = player.uuid() + "|" + generation + "|" + occupancy.vehicleUuid()
                + "|" + occupancy.anchor().blockPosition() + "|" + occupancy.zoneId().value()
                + "|" + occupancy.slotId().map(slot -> slot.value()).orElse("unnamed")
                + "|" + kind.name() + "|" + occupancy.revision().value();
        return new SeatEventId("seat-event:" + Integer.toUnsignedString(key.hashCode(), 16));
    }

    private static String fingerprint(PlayerSeatingEventKind kind, SeatOccupancy occupancy) {
        return "echo_seating:events|" + kind.name() + "|" + occupancy.zoneId().value()
                + occupancy.slotId().map(slot -> "|" + slot.value()).orElse("");
    }
}
