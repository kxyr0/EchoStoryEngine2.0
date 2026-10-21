package io.github.kxyr0.echoanimation.echoscript.procedural.clip;

import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.echoscript.procedural.ProceduralSyntax;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads Key(...).Position(...).Rotation(...).Scale(...) chains.
 */
final class ClipKeyReader {
    KeyDraft read(InvocationExpressionSyntax outer, Map<String, String> trackVariables) {
        List<InvocationExpressionSyntax> modifiers = modifiers(outer);
        InvocationExpressionSyntax key = base(outer);
        if (!CallSyntax.named(key, "Key")) {
            throw new IllegalArgumentException("Expected Key(...) chain, got " + CallSyntax.name(key) + ".");
        }
        String variable = ProceduralSyntax.name(ProceduralSyntax.argument(key, 0, null));
        String track = Optional.ofNullable(trackVariables.get(variable))
                .orElseThrow(() -> new IllegalArgumentException("Unknown track variable '" + variable + "'."));
        int tick = ProceduralSyntax.integer(key, 1, null);
        InterpolationKind interpolation = ProceduralSyntax.optional(key, 2, null)
                .map(ProceduralSyntax::name).map(String::toUpperCase).map(InterpolationKind::valueOf)
                .orElse(InterpolationKind.LINEAR);
        Optional<Vec3> position = vector(modifiers, "Position");
        Optional<Vec3> rotation = vector(modifiers, "Rotation");
        Optional<Vec3> scale = vector(modifiers, "Scale");
        return new KeyDraft(track, tick, position, rotation, scale, interpolation);
    }

    private List<InvocationExpressionSyntax> modifiers(InvocationExpressionSyntax outer) {
        List<InvocationExpressionSyntax> result = new ArrayList<>();
        InvocationExpressionSyntax current = outer;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            result.add(current);
            current = parent;
        }
        Collections.reverse(result);
        return result;
    }

    private InvocationExpressionSyntax base(InvocationExpressionSyntax outer) {
        InvocationExpressionSyntax current = outer;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            current = parent;
        }
        return current;
    }

    private Optional<Vec3> vector(List<InvocationExpressionSyntax> calls, String name) {
        List<InvocationExpressionSyntax> matches = calls.stream().filter(call -> CallSyntax.named(call, name)).toList();
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Key chain contains two " + name + " declarations.");
        }
        for (InvocationExpressionSyntax call : calls) {
            String candidate = CallSyntax.name(call);
            if (!candidate.equals("Position") && !candidate.equals("Rotation") && !candidate.equals("Scale")) {
                throw new IllegalArgumentException("Unsupported Key modifier: " + candidate + ".");
            }
        }
        return matches.stream().findFirst().map(this::vector);
    }

    private Vec3 vector(InvocationExpressionSyntax call) {
        return new Vec3(component(call, 0, "x"), component(call, 1, "y"), component(call, 2, "z"));
    }

    private double component(InvocationExpressionSyntax call, int index, String name) {
        ExpressionSyntax value = ProceduralSyntax.optional(call, -1, name)
                .orElseGet(() -> ProceduralSyntax.argument(call, index, null));
        return ProceduralSyntax.number(value);
    }

    record KeyDraft(
            String track,
            int tick,
            Optional<Vec3> position,
            Optional<Vec3> rotation,
            Optional<Vec3> scale,
            InterpolationKind interpolation
    ) {
    }
}
