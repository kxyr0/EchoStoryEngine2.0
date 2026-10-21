package io.github.kxyr0.echoseating.forge.api.view;

import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import java.util.List;

public interface SeatingEventView {
    List<PlayerSeatEvent> events();
}
