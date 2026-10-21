package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Binds optional View settings without adding runtime scripting or execution. */
final class InspectionViewBinder {
    ItemInspectionView bind(ClassSectionSyntax section, InspectionSource source) {
        InspectionArguments.ordered(section.invocation());
        ItemInspectionView defaults = ItemInspectionView.defaults();
        double scale = defaults.scale();
        double[] rotation = {defaults.rotationX(), defaults.rotationY(), defaults.rotationZ()};
        double[] zoom = {defaults.minZoom(), defaults.maxZoom(), defaults.initialZoom()};
        Set<String> seen = new HashSet<>();
        for (var statement : section.body().statements()) {
            if (!(statement instanceof ExpressionStatementSyntax expression)
                    || !(expression.expression() instanceof InvocationExpressionSyntax call)) {
                throw new BindingFailure("ITEM_INSPECTION_INVALID_VIEW", "View supports only Scale, Rotation, and Zoom.", statement.span());
            }
            String name = InspectionArguments.name(call);
            if (!seen.add(name)) throw InspectionArguments.failure("Repeated View setting: " + name, call);
            switch (name) {
                case "Scale" -> scale = numbers(call, source, "value")[0];
                case "Rotation" -> rotation = numbers(call, source, "x", "y", "z");
                case "Zoom" -> zoom = numbers(call, source, "min", "max", "initial");
                default -> throw InspectionArguments.failure("Unknown View setting: " + name, call);
            }
        }
        try {
            return new ItemInspectionView(scale, rotation[0], rotation[1], rotation[2], zoom[0], zoom[1], zoom[2],
                    defaults.rotationSensitivity(), defaults.zoomSensitivity());
        } catch (IllegalArgumentException failure) {
            throw new BindingFailure("ITEM_INSPECTION_INVALID_VIEW", failure.getMessage(), section.span());
        }
    }

    private double[] numbers(InvocationExpressionSyntax call, InspectionSource source, String... names) {
        List<ArgumentSyntax> args = InspectionArguments.ordered(call, names);
        return args.stream().mapToDouble(value -> InspectionArguments.number(value, source)).toArray();
    }
}
