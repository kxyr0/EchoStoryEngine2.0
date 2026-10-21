package io.github.kxyr0.echoscript.api.syntax;

import io.github.kxyr0.echoscript.api.source.SourceSpan;

/**
 * Common immutable syntax node contract.
 *
 * <p>Every syntax node has a source span. Nodes contain no parser state and no executable behavior.</p>
 */
public interface SyntaxNode {
    SourceSpan span();
}
