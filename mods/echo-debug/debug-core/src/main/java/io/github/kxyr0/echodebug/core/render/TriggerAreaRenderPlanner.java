package io.github.kxyr0.echodebug.core.render;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.List;
import java.util.Optional;

public final class TriggerAreaRenderPlanner {
    private final TriggerFootprintSquares footprint = new TriggerFootprintSquares();

    public Optional<TriggerAreaRenderPlan> plan(
            TriggerAreaDefinitionView area,
            String currentDimension,
            Vector3 camera,
            double maxDistance
    ) {
        if (!area.dimension().equals(currentDimension) || distance(area.geometry().center(), camera) > maxDistance) {
            return Optional.empty();
        }
        List<RenderLine> lines = footprint.lines(area.geometry());
        List<RenderLabel> labels = labels(area);
        return Optional.of(new TriggerAreaRenderPlan(area.triggerId(), area.geometry().shapeKind(),
                area.dimension(), lines, labels, 0));
    }

    private List<RenderLabel> labels(TriggerAreaDefinitionView area) {
        Vector3 max = area.geometry().max();
        Vector3 top = new Vector3(area.geometry().center().X(), max.Y() + 0.5D, area.geometry().center().Z());
        return List.of(new RenderLabel(top, area.triggerId() + "\n" + area.geometry().shapeKind()
                + "\nY " + number(area.geometry().min().Y()) + " -> " + number(max.Y())));
    }

    private static double distance(Vector3 a, Vector3 b) {
        double dx = a.X() - b.X();
        double dy = a.Y() - b.Y();
        double dz = a.Z() - b.Z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
