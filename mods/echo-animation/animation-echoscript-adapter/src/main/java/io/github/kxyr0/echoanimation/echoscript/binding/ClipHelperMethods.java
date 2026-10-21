package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves no-arg EchoScript helper methods used inside face animation clips.
 */
final class ClipHelperMethods {
    private final MethodLookup methods;

    ClipHelperMethods(MethodLookup methods) {
        this.methods = methods;
    }

    InvocationExpressionSyntax surface(InvocationExpressionSyntax call) {
        return resolve(call, List.of("Surface"));
    }

    InvocationExpressionSyntax phase(InvocationExpressionSyntax call) {
        return resolve(call, List.of("Phase", "Enter", "Stay", "Exit"));
    }

    InvocationExpressionSyntax phaseMethod(String name) {
        return resolveName(name, List.of("Phase", "Enter", "Stay", "Exit"), new LinkedHashSet<>());
    }

    Optional<String> rig() {
        return methods.unique("Rig").map(this::rigValue);
    }

    InvocationExpressionSyntax track(InvocationExpressionSyntax call) {
        return resolve(call, List.of("Track"));
    }

    InvocationExpressionSyntax frame(InvocationExpressionSyntax call) {
        return resolve(call, List.of("Frame"));
    }

    private InvocationExpressionSyntax resolve(InvocationExpressionSyntax call, List<String> expected) {
        return resolve(call, expected, new LinkedHashSet<>());
    }

    private InvocationExpressionSyntax resolve(InvocationExpressionSyntax call, List<String> expected,
            Set<String> chain) {
        String name = CallSyntax.name(call);
        InvocationExpressionSyntax root = rootInvocation(call);
        if (expected.contains(CallSyntax.name(root))
                && (!root.arguments().isEmpty() || methods.unique(CallSyntax.name(root)).isEmpty())) {
            return call;
        }
        if (!call.arguments().isEmpty()) {
            throw new IllegalArgumentException("Helper method call must not have arguments: " + name);
        }
        return resolveName(name, expected, chain);
    }

    private InvocationExpressionSyntax rootInvocation(InvocationExpressionSyntax call) {
        InvocationExpressionSyntax current = call;
        while (current.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof InvocationExpressionSyntax parent) {
            current = parent;
        }
        return current;
    }

    private InvocationExpressionSyntax resolveName(String name, List<String> expected, Set<String> chain) {
        MethodSyntax method = methods.unique(name).orElseThrow(() ->
                new IllegalArgumentException("Expected " + String.join("/", expected)
                        + " or helper method returning it: " + name));
        if (!method.parameters().isEmpty()) {
            throw new IllegalArgumentException("Helper method must not declare parameters: " + name);
        }
        if (!chain.add(name)) {
            throw new IllegalArgumentException("Recursive helper method call: " + name);
        }
        return resolve(returnedInvocation(method), expected, chain);
    }

    private String rigValue(MethodSyntax method) {
        if (!method.parameters().isEmpty()) {
            throw new IllegalArgumentException("Rig helper method must not declare parameters.");
        }
        ExpressionSyntax value = returnedExpression(method);
        if (value instanceof StringLiteralExpressionSyntax string) {
            return string.value();
        }
        if (value instanceof InvocationExpressionSyntax call && (CallSyntax.named(call, "FaceRig")
                || CallSyntax.named(call, "Rig"))) {
            return new ArgumentReader().string(call, 0, null).orElseThrow(() ->
                    new IllegalArgumentException("Rig helper must return FaceRig(\"namespace:path\")."));
        }
        throw new IllegalArgumentException("Rig helper must return a string or FaceRig(\"namespace:path\").");
    }

    private ExpressionSyntax returnedExpression(MethodSyntax method) {
        return method.body().statements().stream()
                .filter(ReturnStatementSyntax.class::isInstance)
                .map(ReturnStatementSyntax.class::cast)
                .flatMap(statement -> statement.value().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Helper method must return a value: " + method.name()));
    }

    private InvocationExpressionSyntax returnedInvocation(MethodSyntax method) {
        ExpressionSyntax expression = returnedExpression(method);
        if (expression instanceof InvocationExpressionSyntax invocation) {
            return invocation;
        }
        throw new IllegalArgumentException("Helper method must return an intrinsic: " + method.name());
    }
}
