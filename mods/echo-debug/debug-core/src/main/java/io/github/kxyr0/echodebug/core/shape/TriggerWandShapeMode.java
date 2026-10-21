package io.github.kxyr0.echodebug.core.shape;

import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;

public enum TriggerWandShapeMode {
    BOX(2, AreaShapeKind.BOX),
    SPHERE(2, AreaShapeKind.SPHERE),
    CYLINDER(3, AreaShapeKind.CYLINDER),
    POLYGON(3, AreaShapeKind.POLYGON),
    SEAT(1, null);

    private final int minimumPoints;
    private final AreaShapeKind shapeKind;

    TriggerWandShapeMode(int minimumPoints, AreaShapeKind shapeKind) {
        this.minimumPoints = minimumPoints;
        this.shapeKind = shapeKind;
    }

    public int minimumPoints() {
        return minimumPoints;
    }

    public AreaShapeKind shapeKind() {
        if (this == SEAT) {
            throw new IllegalStateException("Seat selection has no area shape.");
        }
        return shapeKind;
    }

    public boolean acceptsMorePoints(int currentPoints) {
        return this == POLYGON || currentPoints < minimumPoints;
    }

    public boolean complete(int currentPoints) {
        return currentPoints >= minimumPoints;
    }
}
