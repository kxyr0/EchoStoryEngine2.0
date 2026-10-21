package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WeaponArgumentReader {
    private static final List<String> ALLOWED = List.of(
            "id", "name", "description", "tooltip", "texture", "model", "damage", "attackDamage",
            "attacksPerSecond", "attackSpeedModifier", "knockback", "durability", "enchantability",
            "repairIngredient", "unbreakable", "rarity", "glint", "fireResistant", "hiddenFromCreative",
            "equipmentSlot", "creativeOrder"
    );

    Map<String, ArgumentSyntax> read(List<ArgumentSyntax> arguments, String sourcePath,
                                     List<ItemDefinitionDiagnostic> diagnostics) {
        Map<String, ArgumentSyntax> named = new LinkedHashMap<>();
        for (ArgumentSyntax argument : arguments) {
            if (argument.name().isEmpty()) {
                diagnostics.add(error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                        "Weapon accepts named arguments only.", sourcePath));
                continue;
            }
            String name = argument.name().orElseThrow();
            if (named.putIfAbsent(name, argument) != null) {
                diagnostics.add(error(ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                        "Weapon argument is duplicated: " + name, sourcePath));
            }
            if (!ALLOWED.contains(name)) {
                diagnostics.add(error(ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                        "Weapon argument is not supported: " + name, sourcePath));
            }
        }
        validateAliases(named, sourcePath, diagnostics);
        return named;
    }

    private void validateAliases(Map<String, ArgumentSyntax> named, String sourcePath,
                                 List<ItemDefinitionDiagnostic> diagnostics) {
        if (named.containsKey("damage") && named.containsKey("attackDamage")) {
            diagnostics.add(error(ItemDiagnosticCodes.ATTRIBUTE_CONFLICT,
                    "Use only one of damage or attackDamage.", sourcePath));
        }
        if (named.containsKey("attacksPerSecond") && named.containsKey("attackSpeedModifier")) {
            diagnostics.add(error(ItemDiagnosticCodes.ATTRIBUTE_CONFLICT,
                    "Use attacksPerSecond or attackSpeedModifier, not both.", sourcePath));
        }
    }

    private ItemDefinitionDiagnostic error(String code, String message, String sourcePath) {
        return ItemDefinitionDiagnostic.error(code, message, sourcePath);
    }
}
