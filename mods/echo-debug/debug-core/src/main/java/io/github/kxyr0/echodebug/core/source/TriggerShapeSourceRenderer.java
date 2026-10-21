package io.github.kxyr0.echodebug.core.source;

import io.github.kxyr0.echodebug.core.selection.TriggerSelectionGeometry;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.stream.Collectors;

final class TriggerShapeSourceRenderer {
    String render(TriggerWandShapeMode shape, TriggerSelectionGeometry geometry) {
        return switch (shape) {
            case BOX -> "BoxArea(" + TriggerSourceNumbers.vector(geometry.min()) + ", "
                    + TriggerSourceNumbers.vector(geometry.max()) + ")";
            case SPHERE -> "SphereArea(center: " + TriggerSourceNumbers.vector(geometry.center())
                    + ", radius: " + TriggerSourceNumbers.number(geometry.radius().orElseThrow()) + ")";
            case CYLINDER -> cylinder(geometry);
            case POLYGON -> polygon(geometry);
            case SEAT -> throw new IllegalArgumentException("Seat triggers use event subscriptions.");
        };
    }

    private String cylinder(TriggerSelectionGeometry geometry) {
        Vector3 center = new Vector3(geometry.center().X(), geometry.min().Y(), geometry.center().Z());
        return "CylinderArea(center: " + TriggerSourceNumbers.vector(center)
                + ", radius: " + TriggerSourceNumbers.number(geometry.radius().orElseThrow())
                + ", minY: " + TriggerSourceNumbers.number(geometry.min().Y())
                + ", maxY: " + TriggerSourceNumbers.number(geometry.max().Y()) + ")";
    }

    private String polygon(TriggerSelectionGeometry geometry) {
        String points = geometry.polygonPoints().stream()
                .map(TriggerSourceNumbers::vector)
                .collect(Collectors.joining(", "));
        return "PolygonArea(points: [" + points + "], minY: "
                + TriggerSourceNumbers.number(geometry.min().Y()) + ", maxY: "
                + TriggerSourceNumbers.number(geometry.max().Y()) + ")";
    }
}
