package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class SpellConstants {
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Double> numbers = new HashMap<>();
    private final Map<String, Boolean> booleans = new HashMap<>();

    SpellConstants(ClassSyntax clazz) {
        clazz.members().stream().filter(FieldSyntax.class::isInstance).map(FieldSyntax.class::cast).forEach(this::read);
    }

    Optional<String> string(String name) { return Optional.ofNullable(strings.get(name)); }
    Optional<Double> number(String name) { return Optional.ofNullable(numbers.get(name)); }
    Optional<Boolean> bool(String name) { return Optional.ofNullable(booleans.get(name)); }

    private void read(FieldSyntax field) {
        field.initializer().ifPresent(value -> {
            if (value instanceof StringLiteralExpressionSyntax literal) strings.put(field.name(), literal.value());
            else if (value instanceof IntegerLiteralExpressionSyntax literal) numbers.put(field.name(), (double) literal.value());
            else if (value instanceof DecimalLiteralExpressionSyntax literal) numbers.put(field.name(), literal.value().doubleValue());
            else if (value instanceof BooleanLiteralExpressionSyntax literal) booleans.put(field.name(), literal.value());
            else if (value instanceof NameExpressionSyntax name) strings.put(field.name(), name.name());
        });
    }
}

