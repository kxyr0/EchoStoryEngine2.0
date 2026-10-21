package io.github.kxyr0.echodialogue.echoscript.api;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;

/** Public semantic extension for one named Line argument. */
public interface DialogueLineArgumentBinder {
    String argumentName();

    DialogueLineArgumentBinding bind(ExpressionSyntax value);
}
