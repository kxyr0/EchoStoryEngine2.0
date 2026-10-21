package io.github.kxyr0.echowardrobe.core.outfit;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.core.asset.SkinAssetView;
import io.github.kxyr0.echowardrobe.core.binding.WearableCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Semantic binder for OutfitVariantResource scripts.
 */
public final class OutfitVariantBinder {
    public WardrobeResult<List<OutfitVariantDefinition>> bind(
            CompilationUnitSyntax unit,
            String source,
            String fingerprint,
            SkinAssetView assets
    ) {
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        List<OutfitVariantDefinition> variants = unit.classDeclarations().stream()
                .map(resource -> bindClass(resource, source, fingerprint, assets, diagnostics))
                .flatMap(Optional::stream)
                .toList();
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(variants, diagnostics);
    }

    private Optional<OutfitVariantDefinition> bindClass(ClassSyntax resource, String source, String fingerprint,
            SkinAssetView assets, List<WardrobeDiagnostic> diagnostics) {
        if (!"OutfitVariantResource".equals(resource.baseType().name().text())) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID,
                    "Outfit class must derive from OutfitVariantResource.", source));
            return Optional.empty();
        }
        Optional<MethodSyntax> init = initMethod(resource, source, diagnostics);
        if (init.isEmpty()) {
            return Optional.empty();
        }
        OutfitVariantDraft draft = new OutfitVariantDraft();
        init.orElseThrow().body().statements().stream()
                .flatMap(statement -> WearableCallSyntax.statementCall(statement).stream())
                .forEach(call -> apply(call, draft, source, diagnostics));
        if (!required(draft, source, diagnostics)) {
            return Optional.empty();
        }
        OutfitVariantStatus status = status(draft, assets, source, diagnostics);
        try {
            return Optional.of(draft.toDefinition(status, new WearableSourceFingerprint(fingerprint)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID, exception.getMessage(), source));
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
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID,
                    "OutfitVariantResource must contain exactly one Init method.", source));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private void apply(InvocationExpressionSyntax call, OutfitVariantDraft draft, String source,
            List<WardrobeDiagnostic> diagnostics) {
        switch (WearableCallSyntax.name(call)) {
            case "Id" -> draft.id = string(call, 0, "Id", source, diagnostics).orElse(draft.id);
            case "Profile" -> draft.profileId = string(call, 0, "Profile", source, diagnostics)
                    .orElse(draft.profileId);
            case "Match" -> match(call, draft);
            case "Skin" -> skin(call, draft, source, diagnostics);
            case "WorldSkin" -> draft.worldSkin = string(call, 0, "WorldSkin", source, diagnostics)
                    .orElse(draft.worldSkin);
            case "FaceSkin" -> draft.faceSkin = string(call, 0, "FaceSkin", source, diagnostics)
                    .orElse(draft.faceSkin);
            default -> diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID,
                    "Unsupported outfit call: " + WearableCallSyntax.name(call), source));
        }
    }

    private void match(InvocationExpressionSyntax call, OutfitVariantDraft draft) {
        draft.matchSeen = true;
        draft.face = OutfitVariantDraft.selection(WearableCallSyntax.namedString(call, "face").orElse(null));
        draft.top = OutfitVariantDraft.selection(WearableCallSyntax.namedString(call, "top").orElse(null));
        draft.bottom = OutfitVariantDraft.selection(WearableCallSyntax.namedString(call, "bottom").orElse(null));
        draft.feet = OutfitVariantDraft.selection(WearableCallSyntax.namedString(call, "feet").orElse(null));
    }

    private void skin(InvocationExpressionSyntax call, OutfitVariantDraft draft, String source,
            List<WardrobeDiagnostic> diagnostics) {
        string(call, 0, "Skin", source, diagnostics).ifPresent(resource -> {
            draft.worldSkin = resource;
            draft.faceSkin = resource;
        });
    }

    private boolean required(OutfitVariantDraft draft, String source, List<WardrobeDiagnostic> diagnostics) {
        if (draft.id == null || draft.profileId == null || !draft.matchSeen
                || draft.worldSkin == null || draft.faceSkin == null) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID,
                    "Outfit requires Id, Profile, Match, and Skin or WorldSkin/FaceSkin.", source));
            return false;
        }
        return true;
    }

    private OutfitVariantStatus status(OutfitVariantDraft draft, SkinAssetView assets, String source,
            List<WardrobeDiagnostic> diagnostics) {
        boolean world = assets.skinExists(draft.worldSkin);
        boolean face = assets.skinExists(draft.faceSkin);
        if (!world || !face) {
            diagnostics.add(WardrobeDiagnostic.warning(WardrobeDiagnosticCode.WARDROBE_OUTFIT_SKIN_NOT_FOUND,
                    "Outfit skin is not available as a confirmed 64x64 PNG resource.", source));
            return OutfitVariantStatus.MISSING_SKIN;
        }
        return OutfitVariantStatus.ACTIVE;
    }

    private Optional<String> string(InvocationExpressionSyntax call, int index, String label, String source,
            List<WardrobeDiagnostic> diagnostics) {
        Optional<String> value = WearableCallSyntax.positionalString(call, index);
        if (value.isEmpty()) {
            diagnostics.add(error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_INVALID,
                    label + " requires a string literal argument.", source));
        }
        return value;
    }

    private WardrobeDiagnostic error(WardrobeDiagnosticCode code, String message, String source) {
        return WardrobeDiagnostic.error(code, message, source);
    }
}
