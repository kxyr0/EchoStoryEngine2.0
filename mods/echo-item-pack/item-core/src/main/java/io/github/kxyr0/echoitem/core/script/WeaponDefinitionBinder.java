package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionResult;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WeaponResource semantic binder. It reads immutable AST data and never executes source.
 */
public final class WeaponDefinitionBinder {
    private final WeaponCallShape shape = new WeaponCallShape();
    private final WeaponArgumentReader arguments = new WeaponArgumentReader();
    private final WeaponValueReader values = new WeaponValueReader();

    public ItemDefinitionResult<WeaponDefinition> bind(
            CompilationUnitSyntax unit,
            String sourcePath,
            ItemSourceFingerprint fingerprint,
            ItemDefinitionRevision revision
    ) {
        List<ItemDefinitionDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax resource = unit.classDeclaration();
        requireWeaponResource(resource, sourcePath, diagnostics);
        Optional<MethodSyntax> build = buildMethod(resource, sourcePath, diagnostics);
        WeaponExpressionResolver resolver = new WeaponExpressionResolver(resource, sourcePath, diagnostics);
        Optional<InvocationExpressionSyntax> weapon = build.flatMap(method -> weaponReturn(method, resolver,
                sourcePath, diagnostics));
        Optional<WeaponDefinition> definition = weapon.map(call -> readWeapon(call, sourcePath, fingerprint,
                revision, diagnostics));
        if (diagnostics.stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            return ItemDefinitionResult.invalid(diagnostics);
        }
        return ItemDefinitionResult.valid(definition.orElseThrow(), diagnostics);
    }

    private void requireWeaponResource(ClassSyntax resource, String sourcePath,
                                       List<ItemDefinitionDiagnostic> diagnostics) {
        if (!"WeaponResource".equals(resource.baseType().name().text())) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "Source must declare class ... : WeaponResource.", sourcePath));
        }
    }

    private Optional<MethodSyntax> buildMethod(ClassSyntax resource, String sourcePath,
                                               List<ItemDefinitionDiagnostic> diagnostics) {
        List<MethodSyntax> matches = resource.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> "Build".equals(method.name()))
                .toList();
        if (matches.size() != 1) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "WeaponResource must contain exactly one Build method.", sourcePath));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private Optional<InvocationExpressionSyntax> weaponReturn(MethodSyntax build, WeaponExpressionResolver resolver,
                                                              String sourcePath,
                                                              List<ItemDefinitionDiagnostic> diagnostics) {
        if (shape.countWeaponCalls(build.body()) > 1) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "One WeaponResource may contain exactly one Weapon(...).", sourcePath));
        }
        Optional<ExpressionSyntax> resolved = resolver.resolveMethodReturn(build);
        if (resolved.isPresent()) {
            return readReturn(resolved.orElseThrow(), sourcePath, diagnostics);
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                "Build must return Weapon(...).", sourcePath));
        return Optional.empty();
    }

    private Optional<InvocationExpressionSyntax> readReturn(ExpressionSyntax expression, String sourcePath,
                                                            List<ItemDefinitionDiagnostic> diagnostics) {
        if (expression instanceof InvocationExpressionSyntax call && shape.named(call, "Weapon")) {
            return Optional.of(call);
        }
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                "Build must return the Weapon intrinsic.", sourcePath));
        return Optional.empty();
    }

    private WeaponDefinition readWeapon(InvocationExpressionSyntax call, String sourcePath,
                                        ItemSourceFingerprint fingerprint, ItemDefinitionRevision revision,
                                        List<ItemDefinitionDiagnostic> diagnostics) {
        var args = arguments.read(call.arguments(), sourcePath, diagnostics);
        EchoItemId id = readId(args, sourcePath, diagnostics);
        String name = safeText(values.requiredString("name", args, sourcePath, diagnostics), "Unavailable Weapon");
        String texture = safeText(values.requiredString("texture", args, sourcePath, diagnostics),
                "echo_item:item/missing");
        var text = new ItemTextDefinition(name, values.optionalString("description", args, sourcePath,
                diagnostics).orElse(""), values.tooltip(args, sourcePath, diagnostics));
        var visual = new ItemVisualDefinition(texture, values.optionalString("model", args, sourcePath,
                diagnostics).filter(model -> !"handheld".equals(model)), true, 0L);
        var combat = combat(args, sourcePath, diagnostics);
        var durability = durability(args, sourcePath, diagnostics);
        return new WeaponDefinition(id, text, visual, combat, durability, values.rarity(args, sourcePath,
                diagnostics), values.bool("glint", args, false, sourcePath, diagnostics),
                values.bool("fireResistant", args, false, sourcePath, diagnostics),
                values.equipmentSlot(args, sourcePath, diagnostics),
                values.bool("hiddenFromCreative", args, false, sourcePath, diagnostics),
                values.optionalInt("creativeOrder", args, sourcePath, diagnostics), revision, fingerprint, true);
    }

    private EchoItemId readId(java.util.Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
                              String sourcePath, List<ItemDefinitionDiagnostic> diagnostics) {
        String raw = values.requiredString("id", args, sourcePath, diagnostics);
        try {
            return EchoItemId.parseForSource(raw, Path.of(sourcePath));
        } catch (IllegalArgumentException ex) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_ITEM_ID,
                    ex.getMessage(), sourcePath));
            return new EchoItemId("echo_item", "invalid");
        }
    }

    private WeaponCombatDefinition combat(java.util.Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
                                          String sourcePath, List<ItemDefinitionDiagnostic> diagnostics) {
        double damage = values.number(args.containsKey("attackDamage") ? "attackDamage" : "damage", args,
                1.0D, ItemDiagnosticCodes.INVALID_ATTACK_DAMAGE, sourcePath, diagnostics);
        double speed = speed(args, sourcePath, diagnostics);
        double knockback = values.number("knockback", args, 0.0D, ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                sourcePath, diagnostics);
        validateCombat(damage, speed, sourcePath, diagnostics);
        return new WeaponCombatDefinition(clamp(damage, 0.1D, 1024.0D), clamp(speed, 0.1D, 20.0D),
                clamp(knockback, 0.0D, 32.0D));
    }

    private double speed(java.util.Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
                         String sourcePath, List<ItemDefinitionDiagnostic> diagnostics) {
        if (args.containsKey("attackSpeedModifier")) {
            double modifier = values.number("attackSpeedModifier", args, -3.0D,
                    ItemDiagnosticCodes.INVALID_ATTACK_SPEED, sourcePath, diagnostics);
            return modifier + 4.0D;
        }
        return values.number("attacksPerSecond", args, 1.0D, ItemDiagnosticCodes.INVALID_ATTACK_SPEED,
                sourcePath, diagnostics);
    }

    private WeaponDurabilityDefinition durability(
            java.util.Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
            String sourcePath,
            List<ItemDefinitionDiagnostic> diagnostics
    ) {
        int durability = values.integer("durability", args, 250, ItemDiagnosticCodes.INVALID_DURABILITY,
                sourcePath, diagnostics);
        int enchantability = values.integer("enchantability", args, 0, ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                sourcePath, diagnostics);
        values.optionalString("repairIngredient", args, sourcePath, diagnostics);
        boolean unbreakable = values.bool("unbreakable", args, !args.containsKey("durability"), sourcePath,
                diagnostics);
        validateDurability(durability, enchantability, unbreakable, sourcePath, diagnostics);
        return new WeaponDurabilityDefinition(unbreakable && !args.containsKey("durability") ? 0
                : (int) clamp(durability, 1, 100000),
                (int) clamp(enchantability, 0, 100),
                values.optionalString("repairIngredient", args, sourcePath, diagnostics),
                unbreakable);
    }

    private String safeText(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void validateCombat(double damage, double speed, String sourcePath,
                                List<ItemDefinitionDiagnostic> diagnostics) {
        if (damage <= 0.0D || damage > 1024.0D) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_ATTACK_DAMAGE,
                    "attack damage must be positive and bounded.", sourcePath));
        }
        if (speed <= 0.0D || speed > 20.0D) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_ATTACK_SPEED,
                    "attacksPerSecond must be positive and bounded.", sourcePath));
        }
    }

    private void validateDurability(int durability, int enchantability, boolean unbreakable, String sourcePath,
                                    List<ItemDefinitionDiagnostic> diagnostics) {
        if (!unbreakable && (durability <= 0 || durability > 100000)) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_DURABILITY,
                    "durability must be positive and bounded.", sourcePath));
        }
        if (enchantability < 0 || enchantability > 100) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.INVALID_ATTRIBUTE,
                    "enchantability must be bounded.", sourcePath));
        }
    }
}
