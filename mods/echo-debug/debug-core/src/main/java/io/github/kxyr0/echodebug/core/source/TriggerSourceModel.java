package io.github.kxyr0.echodebug.core.source;

import io.github.kxyr0.echodebug.core.path.TriggerIdPlan;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionGeometry;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import java.util.Objects;

public record TriggerSourceModel(
        TriggerIdPlan id,
        String dimension,
        TriggerWandShapeMode shape,
        TriggerSelectionGeometry geometry,
        String scope
) {
    public TriggerSourceModel {
        id = Objects.requireNonNull(id, "id");
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        shape = Objects.requireNonNull(shape, "shape");
        geometry = Objects.requireNonNull(geometry, "geometry");
        scope = Objects.requireNonNull(scope, "scope").trim();
        if (dimension.isEmpty() || scope.isEmpty()) {
            throw new IllegalArgumentException("Source model requires dimension and scope.");
        }
    }

    public static TriggerSourceModel player(TriggerIdPlan id, String dimension,
                                            TriggerWandShapeMode shape, TriggerSelectionGeometry geometry) {
        return new TriggerSourceModel(id, dimension, shape, geometry, "PLAYER");
    }
}
