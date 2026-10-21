package io.github.kxyr0.echotrigger.core.spatial;

import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TriggerSpatialIndex {
    private final List<TriggerArea> areas;

    public TriggerSpatialIndex(List<TriggerArea> areas) {
        this.areas = List.copyOf(Objects.requireNonNull(areas, "areas"));
    }

    public List<TriggerArea> candidates(PlayerSnapshot snapshot) {
        List<TriggerArea> candidates = new ArrayList<>();
        for (TriggerArea area : areas) {
            if (area.dimension().equals(snapshot.dimension())) {
                candidates.add(area);
            }
        }
        return List.copyOf(candidates);
    }

    public int areaCount() {
        return areas.size();
    }
}
