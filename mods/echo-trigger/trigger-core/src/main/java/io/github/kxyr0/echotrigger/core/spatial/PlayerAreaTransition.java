package io.github.kxyr0.echotrigger.core.spatial;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import java.util.List;
import java.util.Optional;

public final class PlayerAreaTransition {
    public List<TriggerEvent> events(
            PlayerSnapshot snapshot,
            TriggerArea area,
            Optional<PlayerAreaMembership> previous,
            long serverTick
    ) {
        boolean inside = area.contains(snapshot.dimension(), snapshot.x(), snapshot.y(), snapshot.z());
        boolean wasInside = previous.map(PlayerAreaMembership::inside).orElse(inside);
        if (!wasInside && inside) {
            return List.of(event(TriggerEventKind.PLAYER_ENTER, snapshot, area, serverTick),
                    event(TriggerEventKind.PLAYER_INSIDE, snapshot, area, serverTick));
        }
        if (wasInside && !inside) {
            return List.of(event(TriggerEventKind.PLAYER_EXIT, snapshot, area, serverTick));
        }
        if (inside) {
            return List.of(event(TriggerEventKind.PLAYER_INSIDE, snapshot, area, serverTick));
        }
        return List.of();
    }

    public PlayerAreaMembership membership(PlayerSnapshot snapshot, TriggerArea area) {
        return new PlayerAreaMembership(snapshot.owner(), area,
                area.contains(snapshot.dimension(), snapshot.x(), snapshot.y(), snapshot.z()));
    }

    private static TriggerEvent event(
            TriggerEventKind kind,
            PlayerSnapshot snapshot,
            TriggerArea area,
            long serverTick
    ) {
        return new TriggerEvent(kind, Optional.of(area), snapshot.owner(), serverTick);
    }
}
