package io.github.kxyr0.echoanimation.echoscript.syntax;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;

/**
 * Small syntax helpers for invocation shapes.
 */
public final class CallSyntax {
    private CallSyntax() {
    }

    public static String name(InvocationExpressionSyntax call) {
        ExpressionSyntax target = call.target();
        if (target instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (target instanceof MemberAccessExpressionSyntax member) {
            return member.memberName();
        }
        return "";
    }

    public static boolean named(InvocationExpressionSyntax call, String name) {
        return name(call).equals(name);
    }
}
