package io.github.kxyr0.echoseating.forge.api.view;

import io.github.kxyr0.echoseating.api.occupancy.SeatOccupancy;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.List;
import java.util.Optional;

public interface SeatOccupancyView {
    Optional<SeatOccupancy> active(PlayerRef player);

    List<SeatOccupancy> activeOccupancies();
}
