package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.geometry.AreaShape;
import io.github.kxyr0.echotrigger.api.model.TriggerArea;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.core.binding.geometry.TriggerGeometryBinder;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TriggerFieldCatalog {
    private final Map<String, TriggerArea> areas;
    private final Map<String, AreaShape> shapes;
    private final Map<String, String> strings;

    private TriggerFieldCatalog(
            Map<String, TriggerArea> areas,
            Map<String, AreaShape> shapes,
            Map<String, String> strings
    ) {
        this.areas = Map.copyOf(areas);
        this.shapes = Map.copyOf(shapes);
        this.strings = Map.copyOf(strings);
    }

    static TriggerFieldCatalog from(Iterable<MemberSyntax> members, List<TriggerDiagnostic> diagnostics) {
        Map<String, TriggerArea> areas = new LinkedHashMap<>();
        Map<String, AreaShape> shapes = new LinkedHashMap<>();
        Map<String, String> strings = new LinkedHashMap<>();
        TriggerGeometryBinder geometry = new TriggerGeometryBinder();
        for (MemberSyntax member : members) {
            if (member instanceof FieldSyntax field) {
                readArea(field).ifPresent(area -> areas.put(field.name(), area));
                readShape(field, geometry, diagnostics).ifPresent(shape -> shapes.put(field.name(), shape));
                readString(field).ifPresent(value -> strings.put(field.name(), value));
            }
        }
        return new TriggerFieldCatalog(areas, shapes, strings);
    }

    Optional<TriggerArea> area(String name) {
        return Optional.ofNullable(areas.get(name));
    }

    Optional<AreaShape> shape(String name) {
        return Optional.ofNullable(shapes.get(name));
    }

    Optional<String> string(String name) {
        return Optional.ofNullable(strings.get(name));
    }

    private static Optional<TriggerArea> readArea(FieldSyntax field) {
        if (!field.type().name().text().equals("Area")
                || field.initializer().isEmpty()
                || !(field.initializer().orElseThrow() instanceof InvocationExpressionSyntax call)
                || !TriggerCallSyntax.name(call).equals("Area")) {
            return Optional.empty();
        }
        return readAreaCall(call);
    }

    static Optional<TriggerArea> readAreaCall(InvocationExpressionSyntax call) {
        Optional<String> dimension = TriggerCallSyntax.positionalString(call, 0);
        Optional<Integer> x = TriggerCallSyntax.positionalInt(call, 1);
        Optional<Integer> y = TriggerCallSyntax.positionalInt(call, 2);
        Optional<Integer> z = TriggerCallSyntax.positionalInt(call, 3);
        if (dimension.isEmpty() || x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        if (call.arguments().size() == 4) {
            return Optional.of(TriggerArea.block(dimension.orElseThrow(), x.orElseThrow(),
                    y.orElseThrow(), z.orElseThrow()));
        }
        return readSizedArea(call, dimension.orElseThrow(), x.orElseThrow(), y.orElseThrow(), z.orElseThrow());
    }

    private static Optional<AreaShape> readShape(
            FieldSyntax field,
            TriggerGeometryBinder geometry,
            List<TriggerDiagnostic> diagnostics
    ) {
        if (field.initializer().isEmpty()
                || !geometry.shapeExpression(field.initializer().orElseThrow())) {
            return Optional.empty();
        }
        return geometry.bind(field.initializer().orElseThrow(), diagnostics);
    }

    private static Optional<String> readString(FieldSyntax field) {
        if (field.initializer().isEmpty()) {
            return Optional.empty();
        }
        ExpressionSyntax initializer = field.initializer().orElseThrow();
        if (initializer instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<TriggerArea> readSizedArea(
            InvocationExpressionSyntax call,
            String dimension,
            int x,
            int y,
            int z
    ) {
        Optional<Integer> sx = TriggerCallSyntax.positionalInt(call, 4);
        Optional<Integer> sy = TriggerCallSyntax.positionalInt(call, 5);
        Optional<Integer> sz = TriggerCallSyntax.positionalInt(call, 6);
        if (sx.isEmpty() || sy.isEmpty() || sz.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TriggerArea(dimension, x, y, z, sx.orElseThrow(), sy.orElseThrow(), sz.orElseThrow()));
    }
}
