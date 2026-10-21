package io.github.kxyr0.echovisibility.script.binding;

import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class VisibilityFieldCatalog {
    private final Map<String, String> strings;

    private VisibilityFieldCatalog(Map<String, String> strings) {
        this.strings = Map.copyOf(strings);
    }

    static VisibilityFieldCatalog from(Iterable<MemberSyntax> members) {
        Map<String, String> strings = new LinkedHashMap<>();
        for (MemberSyntax member : members) {
            if (member instanceof FieldSyntax field && field.initializer().isPresent()
                    && field.initializer().orElseThrow() instanceof StringLiteralExpressionSyntax literal) {
                strings.put(field.name(), literal.value());
            }
        }
        return new VisibilityFieldCatalog(strings);
    }

    Optional<String> resolve(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.ofNullable(strings.get(name.name()));
        }
        return Optional.empty();
    }
}
