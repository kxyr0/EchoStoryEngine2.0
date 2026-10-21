package io.github.kxyr0.echowardrobe.core.binding;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.equipment.ItemRegistryView;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Semantic binder for W2 WearableResource EchoScript files.
 */
public final class WearableDefinitionBinder {
    public WardrobeResult<List<WearableDefinition>> bind(
            CompilationUnitSyntax unit,
            String source,
            String fingerprint,
            ItemRegistryView itemRegistry
    ) {
        Objects.requireNonNull(unit, "unit");
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        List<WearableDefinition> definitions = unit.classDeclarations().stream()
                .map(resource -> bindClass(resource, source, fingerprint, itemRegistry, diagnostics))
                .flatMap(Optional::stream)
                .toList();
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(definitions, diagnostics);
    }

    private Optional<WearableDefinition> bindClass(ClassSyntax resource, String source, String fingerprint,
            ItemRegistryView itemRegistry, List<WardrobeDiagnostic> diagnostics) {
        if (!"WearableResource".equals(resource.baseType().name().text())) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID,
                    "Wearable class must derive from WearableResource.", source));
            return Optional.empty();
        }
        Optional<MethodSyntax> init = initMethod(resource, source, diagnostics);
        if (init.isEmpty()) {
            return Optional.empty();
        }
        WearableDefinitionDraft draft = new WearableDefinitionDraft();
        init.orElseThrow().body().statements().stream()
                .flatMap(statement -> WearableCallSyntax.statementCall(statement).stream())
                .forEach(call -> apply(call, draft, source, diagnostics));
        if (!required(draft, source, diagnostics)) {
            return Optional.empty();
        }
        boolean itemResolved = itemRegistry.itemExists(draft.itemId);
        if (!itemResolved) {
            diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_ITEM_UNRESOLVED,
                    "Item ID is not registered until restart-time registry discovery: " + draft.itemId, source));
        }
        try {
            return Optional.of(draft.toDefinition(itemResolved, new WearableSourceFingerprint(fingerprint)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID, exception.getMessage(), source));
            return Optional.empty();
        }
    }

    private Optional<MethodSyntax> initMethod(ClassSyntax resource, String source,
            List<WardrobeDiagnostic> diagnostics) {
        List<MethodSyntax> matches = resource.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> "Init".equals(method.name()))
                .toList();
        if (matches.size() != 1) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID,
                    "WearableResource must contain exactly one Init method.", source));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private void apply(InvocationExpressionSyntax call, WearableDefinitionDraft draft, String source,
            List<WardrobeDiagnostic> diagnostics) {
        switch (WearableCallSyntax.name(call)) {
            case "Id" -> draft.id = requiredString(call, 0, "Id", source, diagnostics).orElse(draft.id);
            case "Profile" -> draft.profileId = requiredString(call, 0, "Profile", source, diagnostics)
                    .orElse(draft.profileId);
            case "Item" -> draft.itemId = requiredString(call, 0, "Item", source, diagnostics).orElse(draft.itemId);
            case "Slot" -> draft.slot = slot(call, 0, source, diagnostics).orElse(draft.slot);
            case "EquipmentSlot" -> draft.equipmentSlot = equipment(call, 0, source, diagnostics)
                    .orElse(draft.equipmentSlot);
            case "Collection" -> draft.collection = WearableCallSyntax.positionalSymbol(call, 0)
                    .orElse(draft.collection);
            case "Model" -> draft.modelId = requiredString(call, 0, "Model", source, diagnostics)
                    .orElse(draft.modelId);
            case "ModelPosition" -> vector(call, draft.position, "ModelPosition", source, diagnostics);
            case "ModelRotation" -> vector(call, draft.rotation, "ModelRotation", source, diagnostics);
            case "ModelScale" -> vector(call, draft.scale, "ModelScale", source, diagnostics);
            case "PortraitTexture" -> draft.portraitTextureId = requiredString(call, 0, "PortraitTexture",
                    source, diagnostics).orElse(draft.portraitTextureId);
            case "PortraitStrip" -> WearablePortraitSyntax.strip(call, source, diagnostics)
                    .ifPresent(draft.portraitStrips::add);
            default -> diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID,
                    "Unsupported wearable call: " + WearableCallSyntax.name(call), source));
        }
    }

    private boolean required(WearableDefinitionDraft draft, String source, List<WardrobeDiagnostic> diagnostics) {
        if (draft.id == null || draft.profileId == null || draft.itemId == null || draft.slot == null
                || draft.equipmentSlot == null) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID,
                    "Wearable requires Id, Profile, Item, Slot, and EquipmentSlot.", source));
            return false;
        }
        if (draft.slot.equipmentSlot() != draft.equipmentSlot) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_EQUIPMENT_SLOT_MISMATCH,
                    "Wardrobe slot " + draft.slot + " must map to " + draft.slot.equipmentSlot() + ".", source));
            return false;
        }
        return true;
    }

    private Optional<String> requiredString(InvocationExpressionSyntax call, int index, String label, String source,
            List<WardrobeDiagnostic> diagnostics) {
        Optional<String> value = WearableCallSyntax.positionalString(call, index);
        if (value.isEmpty()) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID,
                    label + " requires a string literal argument.", source));
        }
        return value;
    }

    private Optional<WardrobeSlot> slot(InvocationExpressionSyntax call, int index, String source,
            List<WardrobeDiagnostic> diagnostics) {
        return WearableCallSyntax.positionalSymbol(call, index)
                .flatMap(raw -> enumValue(WardrobeSlot.class, raw, WardrobeDiagnosticCode.WARDROBE_SLOT_INVALID,
                        source, diagnostics));
    }

    private Optional<WardrobeEquipmentSlot> equipment(InvocationExpressionSyntax call, int index, String source,
            List<WardrobeDiagnostic> diagnostics) {
        return WearableCallSyntax.positionalSymbol(call, index)
                .flatMap(raw -> enumValue(WardrobeEquipmentSlot.class, raw,
                        WardrobeDiagnosticCode.WARDROBE_EQUIPMENT_SLOT_MISMATCH, source, diagnostics));
    }

    private void vector(InvocationExpressionSyntax call, double[] target, String label, String source,
            List<WardrobeDiagnostic> diagnostics) {
        for (int index = 0; index < 3; index++) {
            Optional<Double> value = WearableCallSyntax.positionalDouble(call, index);
            if (value.isEmpty()) {
                diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_MODEL_TRANSFORM_INVALID,
                        label + " requires three numeric arguments.", source));
                return;
            }
            target[index] = value.orElseThrow();
        }
    }

    private <T extends Enum<T>> Optional<T> enumValue(Class<T> type, String raw, WardrobeDiagnosticCode code,
            String source, List<WardrobeDiagnostic> diagnostics) {
        try {
            return Optional.of(Enum.valueOf(type, raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(code, type.getSimpleName() + " is not supported: " + raw, source));
            return Optional.empty();
        }
    }

    private WardrobeDiagnostic error(WardrobeDiagnosticCode code, String message, String source) {
        return WardrobeDiagnostic.error(code, message, source);
    }
}
