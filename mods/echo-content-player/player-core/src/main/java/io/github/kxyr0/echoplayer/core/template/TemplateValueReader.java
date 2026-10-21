package io.github.kxyr0.echoplayer.core.template;

import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TemplateValueReader {
    String requiredString(String name, Map<String, ArgumentSyntax> args, String source,
                          List<PlayerProfileDiagnostic> diagnostics) {
        Optional<String> value = optionalString(name, args, source, diagnostics);
        if (value.isEmpty()) {
            diagnostics.add(error("Missing required field: " + name, source));
        }
        return value.orElse("");
    }

    Optional<String> optionalString(String name, Map<String, ArgumentSyntax> args, String source,
                                    List<PlayerProfileDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return Optional.empty();
        }
        if (argument.value() instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        diagnostics.add(error(name + " must be a string.", source));
        return Optional.empty();
    }

    String symbol(String name, Map<String, ArgumentSyntax> args, String fallback, String source,
                  List<PlayerProfileDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return fallback;
        }
        ExpressionSyntax value = argument.value();
        if (value instanceof NameExpressionSyntax symbol) {
            return symbol.name();
        }
        return optionalString(name, args, source, diagnostics).orElse(fallback);
    }

    int integer(String name, Map<String, ArgumentSyntax> args, int fallback, String source,
                List<PlayerProfileDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return fallback;
        }
        if (argument.value() instanceof IntegerLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(error(name + " must be an integer.", source));
        return fallback;
    }

    private PlayerProfileDiagnostic error(String message, String source) {
        return PlayerProfileDiagnostic.error("PLAYER_TEMPLATE_BIND_ERROR", message, source);
    }
}
