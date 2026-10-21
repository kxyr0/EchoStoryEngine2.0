package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.api.equipment.ItemEquipmentSlot;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

final class WeaponValueReader {
    String requiredString(String name, Map<String, ArgumentSyntax> args, String sourcePath,
                          List<ItemDefinitionDiagnostic> diagnostics) {
        Optional<String> value = optionalString(name, args, sourcePath, diagnostics);
        if (value.isEmpty()) {
            diagnostics.add(ItemDefinitionDiagnostic.error(requiredCode(name), "Missing required field: " + name,
                    sourcePath));
        }
        return value.orElse("");
    }

    Optional<String> optionalString(String name, Map<String, ArgumentSyntax> args, String sourcePath,
                                    List<ItemDefinitionDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return Optional.empty();
        }
        if (argument.value() instanceof StringLiteralExpressionSyntax literal && !literal.value().isBlank()) {
            return Optional.of(literal.value());
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                name + " must be a non-blank string.", sourcePath));
        return Optional.empty();
    }

    List<String> tooltip(Map<String, ArgumentSyntax> args, String sourcePath,
                         List<ItemDefinitionDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get("tooltip");
        if (argument == null) {
            return List.of();
        }
        if (!(argument.value() instanceof ArrayLiteralExpressionSyntax array)) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "tooltip must be an array of strings.", sourcePath));
            return List.of();
        }
        return array.elements().stream().map(value -> stringElement(value, sourcePath, diagnostics)).toList();
    }

    double number(String name, Map<String, ArgumentSyntax> args, double fallback, String code, String sourcePath,
                  List<ItemDefinitionDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return fallback;
        }
        Optional<Double> value = numberValue(argument.value());
        if (value.isPresent()) {
            return value.orElseThrow();
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(code, name + " must be a number.", sourcePath));
        return fallback;
    }

    int integer(String name, Map<String, ArgumentSyntax> args, int fallback, String code, String sourcePath,
                List<ItemDefinitionDiagnostic> diagnostics) {
        double value = number(name, args, fallback, code, sourcePath, diagnostics);
        if (Math.floor(value) != value) {
            diagnostics.add(ItemDefinitionDiagnostic.error(code, name + " must be an integer.", sourcePath));
        }
        return (int) value;
    }

    boolean bool(String name, Map<String, ArgumentSyntax> args, boolean fallback, String sourcePath,
                 List<ItemDefinitionDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return fallback;
        }
        if (argument.value() instanceof BooleanLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                name + " must be a boolean.", sourcePath));
        return fallback;
    }

    WeaponRarity rarity(Map<String, ArgumentSyntax> args, String sourcePath,
                        List<ItemDefinitionDiagnostic> diagnostics) {
        Optional<String> raw = optionalSymbol("rarity", args, sourcePath, diagnostics);
        if (raw.isEmpty()) {
            return WeaponRarity.COMMON;
        }
        try {
            return WeaponRarity.parse(raw.orElseThrow());
        } catch (IllegalArgumentException ex) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "rarity must be COMMON, UNCOMMON, RARE, or EPIC.", sourcePath));
            return WeaponRarity.COMMON;
        }
    }

    OptionalInt optionalInt(String name, Map<String, ArgumentSyntax> args, String sourcePath,
                            List<ItemDefinitionDiagnostic> diagnostics) {
        if (!args.containsKey(name)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(integer(name, args, 0, ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                sourcePath, diagnostics));
    }

    Optional<ItemEquipmentSlot> equipmentSlot(Map<String, ArgumentSyntax> args, String sourcePath,
                                              List<ItemDefinitionDiagnostic> diagnostics) {
        Optional<String> raw = optionalSymbol("equipmentSlot", args, sourcePath, diagnostics);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ItemEquipmentSlot.parse(raw.orElseThrow()));
        } catch (IllegalArgumentException ex) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                    ex.getMessage(), sourcePath));
            return Optional.empty();
        }
    }

    private Optional<String> optionalSymbol(String name, Map<String, ArgumentSyntax> args, String sourcePath,
                                            List<ItemDefinitionDiagnostic> diagnostics) {
        ArgumentSyntax argument = args.get(name);
        if (argument == null) {
            return Optional.empty();
        }
        if (argument.value() instanceof NameExpressionSyntax symbol) {
            return Optional.of(symbol.name());
        }
        return optionalString(name, args, sourcePath, diagnostics).map(value -> value.toUpperCase(Locale.ROOT));
    }

    private String stringElement(ExpressionSyntax expression, String sourcePath,
                                 List<ItemDefinitionDiagnostic> diagnostics) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return literal.value();
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                "tooltip entries must be strings.", sourcePath));
        return "";
    }

    private Optional<Double> numberValue(ExpressionSyntax value) {
        if (value instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of((double) literal.value());
        }
        if (value instanceof StringLiteralExpressionSyntax literal) {
            return parseStringNumber(literal.value());
        }
        return decimalLiteral(value);
    }

    private Optional<Double> decimalLiteral(ExpressionSyntax value) {
        if (!"io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax"
                .equals(value.getClass().getName())) {
            return Optional.empty();
        }
        try {
            Object raw = value.getClass().getMethod("value").invoke(value);
            if (raw instanceof Number number) {
                return Optional.of(number.doubleValue());
            }
            return raw == null ? Optional.empty() : parseStringNumber(raw.toString());
        } catch (ReflectiveOperationException ex) {
            return Optional.empty();
        }
    }

    private Optional<Double> parseStringNumber(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String requiredCode(String name) {
        return switch (name) {
            case "id" -> ItemDiagnosticCodes.INVALID_ITEM_ID;
            case "name", "texture" -> ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR;
            default -> ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR;
        };
    }
}
