package io.github.kxyr0.echonpcbackpack.script;

import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Objects;

public record NpcBackpackScriptBinding(String canonicalName, InvocationExpressionSyntax call) {
    public NpcBackpackScriptBinding {
        canonicalName = Objects.requireNonNull(canonicalName, "canonicalName");
        call = Objects.requireNonNull(call, "call");
    }
}
