package io.github.kxyr0.echotrigger.forge.authoring;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaSourceLocation;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.AreaShape;
import io.github.kxyr0.echotrigger.api.geometry.CylinderArea;
import io.github.kxyr0.echotrigger.api.geometry.PolygonArea;
import io.github.kxyr0.echotrigger.api.geometry.SphereArea;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.ReactiveTriggerDefinition;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

final class TriggerAreaViewFactory {
    private TriggerAreaViewFactory() {
    }

    static AreaSourceLocation source(io.github.kxyr0.echotrigger.forge.resource.TriggerResourceText resource,
            java.nio.file.Path contentPackRoot) {
        String root = resource.path().toAbsolutePath().normalize().startsWith(contentPackRoot)
                ? "story/content-pack" : "story/triggers";
        return new AreaSourceLocation(root, resource.path().toString());
    }

    static Optional<TriggerAreaDefinitionView> view(
            CompiledTriggerDefinition definition,
            Optional<AreaSourceLocation> source
    ) {
        return definition.reactive().map(reactive -> view(definition, reactive, source));
    }

    private static TriggerAreaDefinitionView view(
            CompiledTriggerDefinition definition,
            ReactiveTriggerDefinition reactive,
            Optional<AreaSourceLocation> source
    ) {
        return new TriggerAreaDefinitionView(definition.id(), reactive.primaryArea().fieldName(),
                reactive.primaryArea().dimension(), definition.revision(),
                geometry(reactive.primaryArea().shape()), source, List.of());
    }

    private static AreaGeometrySnapshot geometry(AreaShape shape) {
        return new AreaGeometrySnapshot(shape.kind(), shape.min(), shape.max(), shape.center(),
                radius(shape), polygonPoints(shape));
    }

    private static OptionalDouble radius(AreaShape shape) {
        if (shape instanceof SphereArea sphere) {
            return OptionalDouble.of(sphere.radius());
        }
        if (shape instanceof CylinderArea cylinder) {
            return OptionalDouble.of(cylinder.radius());
        }
        return OptionalDouble.empty();
    }

    private static List<io.github.kxyr0.echotrigger.api.geometry.Vector3> polygonPoints(AreaShape shape) {
        return shape instanceof PolygonArea polygon ? polygon.points() : List.of();
    }

    static AreaDiagnosticView diagnostic(String code, String message) {
        return AreaDiagnosticView.withoutPosition(code, message);
    }
}
