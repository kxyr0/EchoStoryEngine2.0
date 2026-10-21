package io.github.kxyr0.echovisibility.script.binding;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTarget;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.List;
import java.util.Optional;

final class VisibilityTargetBinder {
    Optional<VisibilityTarget> bind(
            InvocationExpressionSyntax target,
            VisibilityFieldCatalog fields,
            List<VisibilityDiagnostic> diagnostics) {
        return switch (VisibilityCallSyntax.name(target)) {
            case "Npc" -> npc(target, fields, diagnostics).map(VisibilityTarget.class::cast);
            case "Box" -> box(target, diagnostics).map(VisibilityTarget.class::cast);
            default -> invalid(diagnostics, "Target supports only Npc(...) or Box(...).");
        };
    }

    private Optional<VisibilityNpcTarget> npc(
            InvocationExpressionSyntax call,
            VisibilityFieldCatalog fields,
            List<VisibilityDiagnostic> diagnostics) {
        Optional<String> npcId = VisibilityCallSyntax.positional(call, 0).flatMap(fields::resolve);
        Optional<Double> padding = VisibilityCallSyntax.namedText(call, "padding").flatMap(this::parseDouble);
        if (npcId.isEmpty()) {
            return invalid(diagnostics, "Npc(...) requires an NpcId field or namespace:path string.");
        }
        try {
            return Optional.of(new VisibilityNpcTarget(npcId.orElseThrow(), padding.orElse(0.05D)));
        } catch (IllegalArgumentException exception) {
            return invalid(diagnostics, exception.getMessage());
        }
    }

    private Optional<VisibilityBoxTarget> box(
            InvocationExpressionSyntax call,
            List<VisibilityDiagnostic> diagnostics) {
        Optional<String> dimension = VisibilityCallSyntax.namedText(call, "dimension");
        Optional<VisibilityVector3> min = VisibilityCallSyntax.namedCall(call, "min").flatMap(this::position);
        Optional<VisibilityVector3> max = VisibilityCallSyntax.namedCall(call, "max").flatMap(this::position);
        if (dimension.isEmpty() || min.isEmpty() || max.isEmpty()) {
            return invalid(diagnostics, "Box requires dimension, min: Position(...), and max: Position(...).");
        }
        try {
            return Optional.of(new VisibilityBoxTarget(new VisibilityDimensionId(dimension.orElseThrow()),
                    new VisibilityAabb(min.orElseThrow(), max.orElseThrow())));
        } catch (IllegalArgumentException exception) {
            return invalid(diagnostics, exception.getMessage());
        }
    }

    private Optional<VisibilityVector3> position(InvocationExpressionSyntax call) {
        if (!VisibilityCallSyntax.name(call).equals("Position")) {
            return Optional.empty();
        }
        Optional<Double> x = VisibilityCallSyntax.positionalDouble(call, 0);
        Optional<Double> y = VisibilityCallSyntax.positionalDouble(call, 1);
        Optional<Double> z = VisibilityCallSyntax.positionalDouble(call, 2);
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new VisibilityVector3(x.orElseThrow(), y.orElseThrow(), z.orElseThrow()));
    }

    private Optional<Double> parseDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static <T> Optional<T> invalid(List<VisibilityDiagnostic> diagnostics, String message) {
        diagnostics.add(VisibilityDiagnostic.of(VisibilityDiagnosticCode.VISIBILITY_TARGET_NOT_FOUND, message));
        return Optional.empty();
    }
}
