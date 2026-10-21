package io.github.kxyr0.echotrigger.core.spatial;

import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import java.util.Objects;

public record PlayerAreaMembership(TriggerOwnerId owner, TriggerArea area, boolean inside) {
    public PlayerAreaMembership {
        owner = Objects.requireNonNull(owner, "owner");
        area = Objects.requireNonNull(area, "area");
    }
}
