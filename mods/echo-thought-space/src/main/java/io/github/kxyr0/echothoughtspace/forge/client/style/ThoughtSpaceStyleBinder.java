package io.github.kxyr0.echothoughtspace.forge.client.style;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class ThoughtSpaceStyleBinder {
    private static final Set<String> FIELDS = Set.of(
            "zoomInitial", "zoomMin", "zoomMax", "zoomStep", "circleRadius", "nodeFontSize",
            "titleText", "titleSize", "titleX", "titleY");

    Optional<ThoughtSpaceStyleConfig> bind(ClassSyntax resource, ThoughtSpaceStyleConfig fallback) {
        List<ClassCallSyntax> calls = resource.members().stream().filter(ClassCallSyntax.class::isInstance)
                .map(ClassCallSyntax.class::cast)
                .filter(call -> ThoughtScriptCallReader.name(call.invocation()).equals("Style")).toList();
        if (calls.isEmpty()) return Optional.empty();
        if (calls.size() > 1) throw new IllegalArgumentException("Style(...) is declared more than once.");
        Map<String, ExpressionSyntax> values = ThoughtScriptCallReader.named(calls.get(0).invocation());
        values.keySet().stream().filter(name -> !FIELDS.contains(name)).findFirst()
                .ifPresent(name -> { throw new IllegalArgumentException("Unknown Style field: " + name); });
        return Optional.of(new ThoughtSpaceStyleConfig(
                number(values, "zoomInitial", fallback.zoomInitial()),
                number(values, "zoomMin", fallback.zoomMin()),
                number(values, "zoomMax", fallback.zoomMax()),
                number(values, "zoomStep", fallback.zoomStep()),
                number(values, "circleRadius", fallback.circleRadius()),
                number(values, "nodeFontSize", fallback.nodeFontSize()),
                text(values, "titleText", fallback.titleText()),
                number(values, "titleSize", fallback.titleSize()),
                number(values, "titleX", fallback.titleX()),
                number(values, "titleY", fallback.titleY())));
    }

    private static double number(Map<String, ExpressionSyntax> values, String name, double fallback) {
        ExpressionSyntax value = values.get(name);
        return value == null ? fallback : ThoughtScriptCallReader.number(value, name);
    }

    private static String text(Map<String, ExpressionSyntax> values, String name, String fallback) {
        ExpressionSyntax value = values.get(name);
        return value == null ? fallback : ThoughtScriptCallReader.string(value, name);
    }
}
