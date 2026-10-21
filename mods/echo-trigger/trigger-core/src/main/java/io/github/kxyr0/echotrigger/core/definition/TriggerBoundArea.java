package io.github.kxyr0.echotrigger.core.definition;

import io.github.kxyr0.echotrigger.api.geometry.AreaShape;
import java.util.Objects;

public record TriggerBoundArea(String fieldName, String dimension, AreaShape shape) {
    public TriggerBoundArea {
        fieldName = Objects.requireNonNull(fieldName, "fieldName").trim();
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        shape = Objects.requireNonNull(shape, "shape");
        if (fieldName.isEmpty() || dimension.isEmpty()) {
            throw new IllegalArgumentException("Bound area requires fieldName and dimension.");
        }
    }

    public boolean contains(String checkedDimension, io.github.kxyr0.echotrigger.api.geometry.Vector3 point) {
        return dimension.equals(checkedDimension) && shape.contains(point);
    }
}
