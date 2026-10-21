package io.github.kxyr0.echoanimation.echoscript.body;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.frame.FrameTransformReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class BodyClipFrameReader {
    private final ArgumentReader arguments = new ArgumentReader();
    private final FrameTransformReader frameTransforms = new FrameTransformReader();
    private final BodyClipMatrixReader matrices = new BodyClipMatrixReader();

    AnimationKeyframeDefinition read(InvocationExpressionSyntax call) {
        InvocationExpressionSyntax frame = frameTransforms.frame(call);
        require(frame, "Frame");
        requireSupportedModifiers(call);
        InterpolationKind interpolation =
                InterpolationKind.valueOf(
                        arguments.string(frame, -1, "interpolation").orElse("LINEAR"));
        Optional<Matrix4Transform> matrix = matrices.read(modifiers(call));
        Optional<Vec3> position = matrix.isPresent() ? Optional.empty() : position(frame, call);
        Optional<Vec3> rotation = matrix.isPresent() ? Optional.empty() : rotation(frame, call);
        Optional<Vec3> scale = matrix.isPresent() ? Optional.empty() : scale(frame, call);
        return new AnimationKeyframeDefinition(
                arguments.authoredNumber(frame, 0, null, 0),
                position,
                rotation,
                scale,
                matrix,
                interpolation);
    }

    private Optional<Vec3> position(
            InvocationExpressionSyntax frame, InvocationExpressionSyntax call) {
        return modifier(call, "Position", vector(frame, "x", "y", "z", 0), Vec3.ZERO, true);
    }

    private Optional<Vec3> rotation(
            InvocationExpressionSyntax frame, InvocationExpressionSyntax call) {
        return modifier(
                call, "Rotation", vector(frame, "rotX", "rotY", "rotZ", 0), Vec3.ZERO, false);
    }

    private Optional<Vec3> scale(
            InvocationExpressionSyntax frame, InvocationExpressionSyntax call) {
        return modifier(
                call,
                "Scale",
                vector(frame, "scaleX", "scaleY", "scaleZ", 1000),
                new Vec3(1.0D, 1.0D, 1.0D),
                false);
    }

    private Optional<Vec3> vector(
            InvocationExpressionSyntax call, String x, String y, String z, int fallback) {
        if (!arguments.has(call, x) && !arguments.has(call, y) && !arguments.has(call, z)) {
            return Optional.empty();
        }
        return Optional.of(
                new Vec3(
                        arguments.authoredNumber(call, -1, x, fallback),
                        arguments.authoredNumber(call, -1, y, fallback),
                        arguments.authoredNumber(call, -1, z, fallback)));
    }

    private Optional<Vec3> modifier(
            InvocationExpressionSyntax call,
            String name,
            Optional<Vec3> base,
            Vec3 emptyFallback,
            boolean authored) {
        Optional<InvocationExpressionSyntax> modifier =
                modifiers(call).stream()
                        .filter(candidate -> CallSyntax.named(candidate, name))
                        .reduce((left, right) -> right);
        if (modifier.isEmpty()) {
            return base;
        }
        InvocationExpressionSyntax value = modifier.orElseThrow();
        requireKnown(value, List.of("x", "y", "z"));
        Vec3 fallback = base.orElse(emptyFallback);
        return Optional.of(
                new Vec3(
                        number(value, authored, 0, "x").orElse(fallback.x()),
                        number(value, authored, 1, "y").orElse(fallback.y()),
                        number(value, authored, 2, "z").orElse(fallback.z())));
    }

    private Optional<Double> number(
            InvocationExpressionSyntax call, boolean authored, int index, String name) {
        return authored
                ? arguments
                        .authoredNumber(call, -1, name)
                        .or(() -> arguments.authoredNumber(call, index, null))
                : arguments
                        .rawNumber(call, -1, name)
                        .or(() -> arguments.rawNumber(call, index, null));
    }

    private List<InvocationExpressionSyntax> modifiers(InvocationExpressionSyntax call) {
        List<InvocationExpressionSyntax> result = new java.util.ArrayList<>();
        InvocationExpressionSyntax current = call;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            result.add(current);
            current = parent;
        }
        Collections.reverse(result);
        return result;
    }

    private void requireKnown(InvocationExpressionSyntax call, List<String> names) {
        for (ArgumentSyntax argument : call.arguments()) {
            argument.name()
                    .filter(candidate -> !names.contains(candidate))
                    .ifPresent(
                            candidate -> {
                                throw new IllegalArgumentException(
                                        "Unsupported Frame transform field: " + candidate);
                            });
        }
    }

    private void requireSupportedModifiers(InvocationExpressionSyntax call) {
        for (InvocationExpressionSyntax modifier : modifiers(call)) {
            String name = CallSyntax.name(modifier);
            if (!name.equals("Position")
                    && !name.equals("Rotation")
                    && !name.equals("Scale")
                    && !name.equals("Matrix")) {
                throw new IllegalArgumentException("Unsupported Frame transform modifier: " + name);
            }
        }
    }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Expected intrinsic " + name + ".");
        }
    }
}
