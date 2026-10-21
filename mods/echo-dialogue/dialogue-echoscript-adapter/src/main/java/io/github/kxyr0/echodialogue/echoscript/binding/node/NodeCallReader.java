package io.github.kxyr0.echodialogue.echoscript.binding.node;

import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;

interface NodeCallReader {
    boolean read(
            String rootName,
            NodeContentReader.NodeReadState state,
            InvocationExpressionSyntax call);
}
