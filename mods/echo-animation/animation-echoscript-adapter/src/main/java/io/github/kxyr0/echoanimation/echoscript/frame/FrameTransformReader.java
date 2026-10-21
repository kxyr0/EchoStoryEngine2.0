package io.github.kxyr0.echoanimation.echoscript.frame;

import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reads fluent Frame(...).Position(...).Rotation(...) transform modifiers.
 */
public final class FrameTransformReader {
    private static final double ZERO_EPSILON = 0.000001D;
    private final ArgumentReader arguments = new ArgumentReader();

    public InvocationExpressionSyntax frame(InvocationExpressionSyntax call) {
        InvocationExpressionSyntax current = call;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            current = parent;
        }
        return current;
    }

    public Transform3 transform(InvocationExpressionSyntax call) {
        InvocationExpressionSyntax frame = frame(call);
        Vec3 position = basePosition(frame);
        Vec3 scale = baseScale(frame);
        double rotationZ = arguments.authoredNumber(frame, -1, "rotationZ", 0);
        for (InvocationExpressionSyntax modifier : modifiers(call)) {
            String name = CallSyntax.name(modifier);
            if (name.equals("Position")) {
                position = authoredVector(modifier, position);
            } else if (name.equals("Rotation")) {
                rotationZ = rotation(modifier, rotationZ);
            } else if (name.equals("Scale")) {
                scale = rawVector(modifier, scale);
            } else {
                throw new IllegalArgumentException("Unsupported Frame transform modifier: " + name);
            }
        }
        return new Transform3(position, rotationZ, scale, Vec3.ZERO);
    }

    private Vec3 basePosition(InvocationExpressionSyntax call) {
        return new Vec3(arguments.authoredNumber(call, -1, "x", 0),
                arguments.authoredNumber(call, -1, "y", 0),
                arguments.authoredNumber(call, -1, "z", 0));
    }

    private Vec3 baseScale(InvocationExpressionSyntax call) {
        return new Vec3(arguments.authoredNumber(call, -1, "scaleX", 1000),
                arguments.authoredNumber(call, -1, "scaleY", 1000),
                arguments.authoredNumber(call, -1, "scaleZ", 1000));
    }

    private Vec3 authoredVector(InvocationExpressionSyntax call, Vec3 fallback) {
        requireKnown(call, List.of("x", "y", "z"));
        return new Vec3(authored(call, 0, "x").orElse(fallback.x()),
                authored(call, 1, "y").orElse(fallback.y()),
                authored(call, 2, "z").orElse(fallback.z()));
    }

    private Vec3 rawVector(InvocationExpressionSyntax call, Vec3 fallback) {
        requireKnown(call, List.of("x", "y", "z"));
        return new Vec3(raw(call, 0, "x").orElse(fallback.x()),
                raw(call, 1, "y").orElse(fallback.y()),
                raw(call, 2, "z").orElse(fallback.z()));
    }

    private double rotation(InvocationExpressionSyntax call, double fallbackZ) {
        requireKnown(call, List.of("x", "y", "z"));
        if (call.arguments().size() == 1 && call.arguments().get(0).name().isEmpty()) {
            return raw(call, 0, null).orElse(fallbackZ);
        }
        double x = raw(call, 0, "x").orElse(0.0D);
        double y = raw(call, 1, "y").orElse(0.0D);
        if (Math.abs(x) > ZERO_EPSILON || Math.abs(y) > ZERO_EPSILON) {
            throw new IllegalArgumentException("Face Frame Rotation supports only Z; use Rotation(0, 0, z).");
        }
        return raw(call, 2, "z").orElse(fallbackZ);
    }

    private List<InvocationExpressionSyntax> modifiers(InvocationExpressionSyntax call) {
        List<InvocationExpressionSyntax> result = new ArrayList<>();
        InvocationExpressionSyntax current = call;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            result.add(current);
            current = parent;
        }
        Collections.reverse(result);
        return result;
    }

    private Optional<Double> authored(InvocationExpressionSyntax call, int index, String name) {
        return arguments.authoredNumber(call, -1, name).or(() -> arguments.authoredNumber(call, index, null));
    }

    private Optional<Double> raw(InvocationExpressionSyntax call, int index, String name) {
        return name == null ? arguments.rawNumber(call, index, null)
                : arguments.rawNumber(call, -1, name).or(() -> arguments.rawNumber(call, index, null));
    }

    private void requireKnown(InvocationExpressionSyntax call, List<String> names) {
        for (ArgumentSyntax argument : call.arguments()) {
            argument.name().filter(name -> !names.contains(name)).ifPresent(name -> {
                throw new IllegalArgumentException("Unsupported Frame transform field: " + name);
            });
        }
    }
}
