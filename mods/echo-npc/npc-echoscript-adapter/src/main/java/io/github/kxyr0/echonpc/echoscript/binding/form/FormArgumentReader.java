package io.github.kxyr0.echonpc.echoscript.binding.form;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.binding.SemanticDiagnosticFactory;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FormArgumentReader {
    private final FormCallShape shape = new FormCallShape();

    Map<String, ArgumentSyntax> named(List<ArgumentSyntax> arguments, Set<String> allowed,
            List<NpcDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = new LinkedHashMap<>();
        for (ArgumentSyntax argument : arguments) {
            if (argument.name().isEmpty()) {
                diagnostics.add(error("NPC_FORM_POSITIONAL_ARG", "Form intrinsic принимает только named args.",
                        argument));
                continue;
            }
            String name = argument.name().orElseThrow();
            if (named.putIfAbsent(name, argument) != null) {
                diagnostics.add(error("NPC_FORM_DUPLICATE_ARG", "Аргумент указан повторно.", argument));
            }
            if (!allowed.contains(name)) {
                diagnostics.add(error("NPC_FORM_UNSUPPORTED_ARG", "Аргумент не поддерживается.", argument));
            }
        }
        return named;
    }

    String string(String name, Map<String, ArgumentSyntax> args, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        if (arg == null) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_MISSING_" + name.toUpperCase(java.util.Locale.ROOT),
                    "Обязательный аргумент формы отсутствует.", name));
            return "";
        }
        return string(arg, diagnostics);
    }

    Optional<String> optionalString(String name, Map<String, ArgumentSyntax> args, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        return arg == null ? Optional.empty() : Optional.of(string(arg, diagnostics));
    }

    double number(String name, Map<String, ArgumentSyntax> args, double fallback, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        return arg == null ? fallback : number(arg, diagnostics);
    }

    Optional<InvocationExpressionSyntax> call(String name, String callName, Map<String, ArgumentSyntax> args,
            List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        if (arg == null) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_MISSING_" + name.toUpperCase(java.util.Locale.ROOT),
                    "Обязательный аргумент формы отсутствует.", name));
            return Optional.empty();
        }
        if (arg.value() instanceof InvocationExpressionSyntax call && shape.named(call, callName)) {
            return Optional.of(call);
        }
        diagnostics.add(error("NPC_FORM_CALL_EXPECTED", "Ожидался вызов " + callName + "(...).", arg));
        return Optional.empty();
    }

    List<InvocationExpressionSyntax> callArray(String name, String callName, Map<String, ArgumentSyntax> args,
            List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        if (arg == null) {
            return List.of();
        }
        if (!(arg.value() instanceof ArrayLiteralExpressionSyntax array)) {
            diagnostics.add(error("NPC_FORM_ARRAY_EXPECTED", "Ожидался массив вызовов.", arg));
            return List.of();
        }
        return array.elements().stream().flatMap(value -> callElement(value, callName, arg, diagnostics).stream())
                .toList();
    }

    List<String> stringArray(String name, Map<String, ArgumentSyntax> args, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax arg = args.get(name);
        if (arg == null) {
            return List.of();
        }
        if (!(arg.value() instanceof ArrayLiteralExpressionSyntax array)) {
            diagnostics.add(error("NPC_FORM_TAGS_ARRAY", "tags должен быть массивом строк.", arg));
            return List.of();
        }
        return array.elements().stream().flatMap(value -> stringElement(value, arg, diagnostics).stream()).toList();
    }

    private Optional<InvocationExpressionSyntax> callElement(ExpressionSyntax value, String callName,
            ArgumentSyntax arg, List<NpcDiagnostic> diagnostics) {
        if (value instanceof InvocationExpressionSyntax call && shape.named(call, callName)) {
            return Optional.of(call);
        }
        diagnostics.add(error("NPC_FORM_ARRAY_CALL_EXPECTED", "Элемент массива должен быть " + callName + "(...).",
                arg));
        return Optional.empty();
    }

    private Optional<String> stringElement(ExpressionSyntax value, ArgumentSyntax arg,
            List<NpcDiagnostic> diagnostics) {
        if (value instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        diagnostics.add(error("NPC_FORM_STRING_ARRAY_ITEM", "Элемент массива должен быть строкой.", arg));
        return Optional.empty();
    }

    String skinCall(InvocationExpressionSyntax call, List<NpcDiagnostic> diagnostics) {
        if (call.arguments().size() == 1 && call.arguments().get(0).name().isEmpty()) {
            return string(call.arguments().get(0), diagnostics);
        }
        diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_SKIN_ARGS",
                "Skin принимает один positional string argument.", "Skin", call.span()));
        return "";
    }

    private String string(ArgumentSyntax arg, List<NpcDiagnostic> diagnostics) {
        if (arg.value() instanceof StringLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(error("NPC_FORM_STRING_EXPECTED", "Ожидалась строка.", arg));
        return "";
    }

    private double number(ArgumentSyntax arg, List<NpcDiagnostic> diagnostics) {
        if (arg.value() instanceof DecimalLiteralExpressionSyntax decimal) {
            return decimal.value().doubleValue();
        }
        if (arg.value() instanceof IntegerLiteralExpressionSyntax integer) {
            return integer.value();
        }
        diagnostics.add(error("NPC_FORM_NUMBER_EXPECTED", "Ожидалось число.", arg));
        return 0.0D;
    }

    private NpcDiagnostic error(String code, String message, ArgumentSyntax argument) {
        return SemanticDiagnosticFactory.error(code, message, argument.name().orElse("<positional>"),
                argument.span());
    }
}
