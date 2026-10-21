package io.github.kxyr0.echonpc.echoscript.binding;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class NpcArgumentReader {
    private static final String NAME_COLOR = "nameColor";

    Map<String, ArgumentSyntax> read(List<ArgumentSyntax> arguments, List<NpcDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = new LinkedHashMap<>();
        for (ArgumentSyntax argument : arguments) {
            if (argument.name().isEmpty()) {
                if (isNameColorCall(argument.value())) {
                    putNamed(named, NAME_COLOR, argument, diagnostics);
                    continue;
                }
                diagnostics.add(error("ENPC-NPCRESOURCE-POSITIONAL",
                        "Npc принимает только named arguments.", argument));
                continue;
            }
            String name = argument.name().orElseThrow();
            putNamed(named, name, argument, diagnostics);
            if (!List.of("id", "display", "skin", NAME_COLOR, "defaultForm").contains(name)) {
                diagnostics.add(error("ENPC-NPCRESOURCE-UNSUPPORTED-ARG",
                        "Аргумент Npc не поддерживается.", argument));
            }
        }
        return named;
    }

    String stringValue(String name, Map<String, ArgumentSyntax> arguments, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax argument = arguments.get(name);
        if (argument == null) {
            diagnostics.add(NpcDiagnostic.error("ENPC-NPCRESOURCE-MISSING-" + name.toUpperCase(java.util.Locale.ROOT),
                    "Обязательный аргумент отсутствует.", name));
            return "";
        }
        if (argument.value() instanceof StringLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-STRING-EXPECTED", "Ожидалась строка.", argument));
        return "";
    }

    String skinValue(Map<String, ArgumentSyntax> arguments, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax argument = arguments.get("skin");
        if (argument == null) {
            return "";
        }
        if (argument.value() instanceof InvocationExpressionSyntax call && new NpcCallShape().named(call, "Skin")) {
            return readSkinCall(call, diagnostics);
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-SKIN-CALL", "skin должен быть Skin(\"namespace:path\").", argument));
        return "";
    }

    String defaultFormValue(Map<String, ArgumentSyntax> arguments, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax argument = arguments.get("defaultForm");
        if (argument == null) {
            return "";
        }
        if (argument.value() instanceof InvocationExpressionSyntax call
                && new NpcCallShape().named(call, "DefaultForm")) {
            return readSingleStringCall(call, diagnostics, "DefaultForm", "defaultForm");
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-DEFAULT-FORM-CALL",
                "defaultForm должен быть DefaultForm(\"namespace:path\").", argument));
        return "";
    }

    Optional<NpcNameColor> nameColorValue(Map<String, ArgumentSyntax> arguments, List<NpcDiagnostic> diagnostics) {
        ArgumentSyntax argument = arguments.get(NAME_COLOR);
        if (argument == null) {
            return Optional.empty();
        }
        if (argument.value() instanceof InvocationExpressionSyntax call && isNameColorCall(call)) {
            return readNameColorCall(call, diagnostics);
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-NAME-COLOR-CALL",
                "nameColor must be nameColor(\"#RRGGBB\") or opaque nameColor(\"#RRGGBBAA\").",
                argument));
        return Optional.empty();
    }

    private void putNamed(
            Map<String, ArgumentSyntax> named,
            String name,
            ArgumentSyntax argument,
            List<NpcDiagnostic> diagnostics
    ) {
        if (named.putIfAbsent(name, argument) != null) {
            diagnostics.add(error("ENPC-NPCRESOURCE-DUPLICATE-ARG",
                    "Аргумент Npc указан повторно.", argument));
        }
    }

    private String readSkinCall(InvocationExpressionSyntax call, List<NpcDiagnostic> diagnostics) {
        return readSingleStringCall(call, diagnostics, "Skin", "skin");
    }

    private String readSingleStringCall(InvocationExpressionSyntax call, List<NpcDiagnostic> diagnostics,
            String callName, String context) {
        List<ArgumentSyntax> args = new ArrayList<>(call.arguments());
        if (args.size() != 1 || args.get(0).name().isPresent()) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-" + context.toUpperCase()
                    + "-ARGS", callName + " принимает один positional string argument.", callName, call.span()));
            return "";
        }
        if (args.get(0).value() instanceof StringLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-" + context.toUpperCase() + "-STRING",
                callName + " принимает строковый ID.", args.get(0)));
        return "";
    }

    private Optional<NpcNameColor> readNameColorCall(
            InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics
    ) {
        List<ArgumentSyntax> args = new ArrayList<>(call.arguments());
        if (args.size() != 1 || args.get(0).name().isPresent()) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-NAME-COLOR-ARGS",
                    "nameColor принимает один positional string argument.", NAME_COLOR, call.span()));
            return Optional.empty();
        }
        if (args.get(0).value() instanceof StringLiteralExpressionSyntax literal) {
            return parseNameColor(literal.value(), call, diagnostics);
        }
        diagnostics.add(error("ENPC-NPCRESOURCE-NAME-COLOR-STRING",
                "nameColor принимает строковый hex.", args.get(0)));
        return Optional.empty();
    }

    private Optional<NpcNameColor> parseNameColor(
            String value,
            InvocationExpressionSyntax call,
            List<NpcDiagnostic> diagnostics
    ) {
        try {
            return Optional.of(NpcNameColor.parse(value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-NAME-COLOR-INVALID",
                    "nameColor имеет неверный hex-формат.", value, call.span()));
            return Optional.empty();
        }
    }

    private boolean isNameColorCall(ExpressionSyntax expression) {
        return expression instanceof InvocationExpressionSyntax call && isNameColorCall(call);
    }

    private boolean isNameColorCall(InvocationExpressionSyntax call) {
        NpcCallShape shape = new NpcCallShape();
        return shape.named(call, NAME_COLOR) || shape.named(call, "NameColor");
    }

    private NpcDiagnostic error(String code, String message, ArgumentSyntax argument) {
        return SemanticDiagnosticFactory.error(code, message, argument.name().orElse("<positional>"), argument.span());
    }
}
