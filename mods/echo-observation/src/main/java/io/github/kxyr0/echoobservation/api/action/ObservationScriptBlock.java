package io.github.kxyr0.echoobservation.api.action;

import java.util.List;
import java.util.Objects;

public record ObservationScriptBlock(List<ObservationScriptAction> actions) {
    public static final ObservationScriptBlock EMPTY = new ObservationScriptBlock(List.of());

    public ObservationScriptBlock {
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }
}
