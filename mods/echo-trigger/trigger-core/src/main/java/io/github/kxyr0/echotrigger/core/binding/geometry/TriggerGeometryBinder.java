package io.github.kxyr0.echotrigger.core.binding.geometry;

import io.github.kxyr0.echotrigger.api.geometry.AreaShape;
import io.github.kxyr0.echotrigger.api.geometry.BoxArea;
import io.github.kxyr0.echotrigger.api.geometry.CylinderArea;
import io.github.kxyr0.echotrigger.api.geometry.PolygonArea;
import io.github.kxyr0.echotrigger.api.geometry.SphereArea;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TriggerGeometryBinder {
    public Optional<AreaShape> bind(ExpressionSyntax expression, List<TriggerDiagnostic> diagnostics) {
        if (expression instanceof InvocationExpressionSyntax call) {
            return bind(TriggerCallSyntax.name(call), call.arguments(), diagnostics);
        }
        if (expression instanceof ObjectCreationExpressionSyntax creation) {
            return bind(TriggerCallSyntax.creationName(creation), creation.arguments(), diagnostics);
        }
        return Optional.empty();
    }

    public boolean shapeExpression(ExpressionSyntax expression) {
        String name = expression instanceof InvocationExpressionSyntax call ? TriggerCallSyntax.name(call) : "";
        if (expression instanceof ObjectCreationExpressionSyntax creation) {
            name = TriggerCallSyntax.creationName(creation);
        }
        return name.equals("BoxArea") || name.equals("SphereArea") || name.equals("CylinderArea")
                || name.equals("CircleArea") || name.equals("PolygonArea");
    }

    private Optional<AreaShape> bind(
            String name,
            List<ArgumentSyntax> args,
            List<TriggerDiagnostic> diagnostics
    ) {
        return switch (name) {
            case "BoxArea" -> box(args, diagnostics);
            case "SphereArea" -> sphere(args, diagnostics);
            case "CylinderArea", "CircleArea" -> cylinder(args, diagnostics);
            case "PolygonArea" -> polygon(args, diagnostics);
            default -> Optional.empty();
        };
    }

    private Optional<AreaShape> box(List<ArgumentSyntax> args, List<TriggerDiagnostic> diagnostics) {
        Optional<Vector3> min = namedVector(args, "min").or(() -> vectorArg(args, 0));
        Optional<Vector3> max = namedVector(args, "max").or(() -> vectorArg(args, 1));
        if (min.isEmpty() || max.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_AREA_SHAPE_INVALID,
                    "BoxArea requires min and max Vector3 values."));
            return Optional.empty();
        }
        return guarded(() -> new BoxArea(min.orElseThrow(), max.orElseThrow()),
                TriggerDiagnosticCode.TRIGGER_AREA_MIN_MAX_INVALID, diagnostics);
    }

    private Optional<AreaShape> sphere(List<ArgumentSyntax> args, List<TriggerDiagnostic> diagnostics) {
        Optional<Vector3> center = namedVector(args, "center").or(() -> vectorArg(args, 0));
        Optional<Double> radius = namedDouble(args, "radius").or(() -> doubleArg(args, 1));
        if (center.isEmpty() || radius.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_AREA_SHAPE_INVALID,
                    "SphereArea requires center Vector3 and radius."));
            return Optional.empty();
        }
        return guarded(() -> new SphereArea(center.orElseThrow(), radius.orElseThrow()),
                TriggerDiagnosticCode.TRIGGER_AREA_RADIUS_INVALID, diagnostics);
    }

    private Optional<AreaShape> cylinder(List<ArgumentSyntax> args, List<TriggerDiagnostic> diagnostics) {
        Optional<Vector3> center = namedVector(args, "center").or(() -> vectorArg(args, 0));
        Optional<Double> radius = namedDouble(args, "radius").or(() -> doubleArg(args, 1));
        if (center.isEmpty() || radius.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_AREA_SHAPE_INVALID,
                    "CylinderArea requires center Vector3 and radius."));
            return Optional.empty();
        }
        double minY = namedDouble(args, "minY").or(() -> doubleArg(args, 2))
                .orElse(center.orElseThrow().Y());
        double maxY = namedDouble(args, "maxY").or(() -> doubleArg(args, 3)).orElse(minY + 1.0D);
        return guarded(() -> new CylinderArea(center.orElseThrow(), radius.orElseThrow(), minY, maxY),
                TriggerDiagnosticCode.TRIGGER_AREA_RADIUS_INVALID, diagnostics);
    }

    private Optional<AreaShape> polygon(List<ArgumentSyntax> args, List<TriggerDiagnostic> diagnostics) {
        List<Vector3> points = points(args);
        if (points.isEmpty()) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_POLYGON_INVALID,
                    "PolygonArea requires an array of Vector3 points."));
            return Optional.empty();
        }
        double minY = namedDouble(args, "minY").or(() -> doubleArg(args, 1)).orElse(points.get(0).Y());
        double maxY = namedDouble(args, "maxY").or(() -> doubleArg(args, 2)).orElse(minY + 1.0D);
        return guarded(() -> new PolygonArea(points, minY, maxY),
                TriggerDiagnosticCode.TRIGGER_POLYGON_INVALID, diagnostics);
    }

    private List<Vector3> points(List<ArgumentSyntax> args) {
        Optional<ExpressionSyntax> expression = value(args, 0).or(() -> named(args, "points"));
        if (expression.isEmpty() || !(expression.orElseThrow() instanceof ArrayLiteralExpressionSyntax array)) {
            return List.of();
        }
        List<Vector3> points = new ArrayList<>();
        for (ExpressionSyntax element : array.elements()) {
            Optional<Vector3> point = vector(element);
            if (point.isEmpty()) {
                return List.of();
            }
            points.add(point.orElseThrow());
        }
        return List.copyOf(points);
    }

    private Optional<Vector3> vectorArg(List<ArgumentSyntax> args, int index) {
        return value(args, index).flatMap(this::vector);
    }

    private Optional<Vector3> namedVector(List<ArgumentSyntax> args, String name) {
        return named(args, name).flatMap(this::vector);
    }

    private Optional<Vector3> vector(ExpressionSyntax expression) {
        if (expression instanceof InvocationExpressionSyntax call && TriggerCallSyntax.name(call).equals("Vector3")) {
            return vector(call.arguments());
        }
        if (expression instanceof ObjectCreationExpressionSyntax creation
                && TriggerCallSyntax.creationName(creation).equals("Vector3")) {
            return vector(creation.arguments());
        }
        return Optional.empty();
    }

    private Optional<Vector3> vector(List<ArgumentSyntax> args) {
        Optional<Double> x = doubleArg(args, 0);
        Optional<Double> y = doubleArg(args, 1);
        Optional<Double> z = doubleArg(args, 2);
        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(new Vector3(x.orElseThrow(), y.orElseThrow(), z.orElseThrow()))
                : Optional.empty();
    }

    private Optional<Double> namedDouble(List<ArgumentSyntax> args, String name) {
        return named(args, name).flatMap(TriggerCallSyntax::doubleValue);
    }

    private Optional<Double> doubleArg(List<ArgumentSyntax> args, int index) {
        return value(args, index).flatMap(TriggerCallSyntax::doubleValue);
    }

    private Optional<ExpressionSyntax> value(List<ArgumentSyntax> args, int index) {
        return index < args.size() ? Optional.of(args.get(index).value()) : Optional.empty();
    }

    private Optional<ExpressionSyntax> named(List<ArgumentSyntax> args, String name) {
        return args.stream().filter(arg -> arg.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).findFirst();
    }

    private Optional<AreaShape> guarded(
            ShapeFactory factory,
            TriggerDiagnosticCode code,
            List<TriggerDiagnostic> diagnostics
    ) {
        try {
            return Optional.of(factory.create());
        } catch (IllegalArgumentException exception) {
            diagnostics.add(diagnostic(code, exception.getMessage()));
            return Optional.empty();
        }
    }

    private static TriggerDiagnostic diagnostic(TriggerDiagnosticCode code, String message) {
        return new TriggerDiagnostic(code, message);
    }

    @FunctionalInterface
    private interface ShapeFactory {
        AreaShape create();
    }
}
