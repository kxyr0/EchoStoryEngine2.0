package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/**
 * Immutable member-access expression syntax.
 */
public record MemberAccessExpressionSyntax(
        ExpressionSyntax target,
        String memberName,
        SourceSpan span
) implements ExpressionSyntax {
    public MemberAccessExpressionSyntax {
        target = Objects.requireNonNull(target, "target");
        memberName = Objects.requireNonNull(memberName, "memberName");
        span = Objects.requireNonNull(span, "span");
        if (memberName.isBlank()) {
            throw new IllegalArgumentException("Member name must not be blank.");
        }
    }
}
