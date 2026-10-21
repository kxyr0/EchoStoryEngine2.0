package io.github.kxyr0.echowardrobe.api.selection;

import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed outfit slot selection. The empty case is a value, not a string sentinel.
 */
public record WearableSelection(Optional<WearableId> wearableId) {
    public static final WearableSelection EMPTY = new WearableSelection(Optional.empty());

    public WearableSelection {
        wearableId = Objects.requireNonNull(wearableId, "wearableId");
    }

    public static WearableSelection of(WearableId id) {
        return new WearableSelection(Optional.of(Objects.requireNonNull(id, "id")));
    }

    public String serialize() {
        return wearableId.map(WearableId::value).orElse("-");
    }
}
