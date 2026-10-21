package io.github.kxyr0.echoanimation.echoscript.body;

import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.echoscript.binding.ArgumentReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;

import java.util.List;
import java.util.Optional;

final class BodyClipMatrixReader {
    private final ArgumentReader arguments = new ArgumentReader();

    Optional<Matrix4Transform> read(List<InvocationExpressionSyntax> modifiers) {
        Optional<InvocationExpressionSyntax> modifier =
                modifiers.stream()
                        .filter(candidate -> CallSyntax.named(candidate, "Matrix"))
                        .reduce((left, right) -> right);
        if (modifier.isEmpty()) {
            return Optional.empty();
        }
        InvocationExpressionSyntax matrix = modifier.orElseThrow();
        if (matrix.arguments().size() != 16) {
            throw new IllegalArgumentException("Matrix requires exactly 16 row-major values.");
        }
        double[] values = new double[16];
        for (int index = 0; index < values.length; index++) {
            ArgumentSyntax argument = matrix.arguments().get(index);
            if (argument.name().isPresent()) {
                throw new IllegalArgumentException(
                        "Matrix accepts positional row-major values only.");
            }
            Optional<Double> value = arguments.rawNumber(matrix, index, null);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Matrix value " + index + " is missing.");
            }
            values[index] = value.orElseThrow();
        }
        return Optional.of(Matrix4Transform.fromRowMajor(values));
    }
}
