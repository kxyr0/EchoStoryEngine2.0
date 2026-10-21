package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.value;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.Optional;

public final class IntrinsicReferenceReader {
    private IntrinsicReferenceReader() {
    }

    public static Optional<String> read(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax value) {
            return Optional.of(value.value());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        if (expression instanceof MemberAccessExpressionSyntax access) {
            return memberPath(access);
        }
        return Optional.empty();
    }

    private static Optional<String> memberPath(MemberAccessExpressionSyntax access) {
        return read(access.target()).map(target -> target + "." + access.memberName());
    }
}
