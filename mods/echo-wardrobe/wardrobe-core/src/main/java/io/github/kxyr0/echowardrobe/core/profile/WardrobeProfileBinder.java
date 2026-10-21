package io.github.kxyr0.echowardrobe.core.profile;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.profile.CoveragePolicy;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.binding.WearableCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Semantic binder for WardrobeProfileResource scripts.
 */
public final class WardrobeProfileBinder {
    public WardrobeResult<List<WardrobeProfileDefinition>> bind(
            CompilationUnitSyntax unit,
            String source,
            String fingerprint
    ) {
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        List<WardrobeProfileDefinition> profiles = unit.classDeclarations().stream()
                .map(resource -> bindClass(resource, source, fingerprint, diagnostics))
                .flatMap(Optional::stream)
                .toList();
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(profiles, diagnostics);
    }

    private Optional<WardrobeProfileDefinition> bindClass(ClassSyntax resource, String source, String fingerprint,
            List<WardrobeDiagnostic> diagnostics) {
        if (!"WardrobeProfileResource".equals(resource.baseType().name().text())) {
            diagnostics.add(error("Profile class must derive from WardrobeProfileResource.", source));
            return Optional.empty();
        }
        Optional<MethodSyntax> init = initMethod(resource, source, diagnostics);
        if (init.isEmpty()) {
            return Optional.empty();
        }
        WardrobeProfileDraft draft = new WardrobeProfileDraft();
        init.orElseThrow().body().statements().stream()
                .flatMap(statement -> WearableCallSyntax.statementCall(statement).stream())
                .forEach(call -> apply(call, draft, source, diagnostics));
        if (draft.id == null || draft.baseSkin == null) {
            diagnostics.add(error("Profile requires Id and BaseSkin.", source));
            return Optional.empty();
        }
        try {
            return Optional.of(draft.toDefinition(new WearableSourceFingerprint(fingerprint)));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(exception.getMessage(), source));
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
            diagnostics.add(error("WardrobeProfileResource must contain exactly one Init method.", source));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private void apply(InvocationExpressionSyntax call, WardrobeProfileDraft draft, String source,
            List<WardrobeDiagnostic> diagnostics) {
        switch (WearableCallSyntax.name(call)) {
            case "Id" -> draft.id = string(call, 0, "Id", source, diagnostics).orElse(draft.id);
            case "BaseSkin" -> draft.baseSkin = string(call, 0, "BaseSkin", source, diagnostics)
                    .orElse(draft.baseSkin);
            case "Coverage" -> draft.coveragePolicy = symbol(call, 0, CoveragePolicy.class, source, diagnostics)
                    .orElse(draft.coveragePolicy);
            case "Slots" -> slots(call, draft, source, diagnostics);
            default -> diagnostics.add(error("Unsupported profile call: " + WearableCallSyntax.name(call), source));
        }
    }

    private void slots(InvocationExpressionSyntax call, WardrobeProfileDraft draft, String source,
            List<WardrobeDiagnostic> diagnostics) {
        for (int index = 0; index < WearableCallSyntax.argumentCount(call); index++) {
            symbol(call, index, WardrobeSlot.class, source, diagnostics).ifPresent(draft.slots::add);
        }
    }

    private Optional<String> string(InvocationExpressionSyntax call, int index, String label, String source,
            List<WardrobeDiagnostic> diagnostics) {
        Optional<String> value = WearableCallSyntax.positionalString(call, index);
        if (value.isEmpty()) {
            diagnostics.add(error(label + " requires a string literal argument.", source));
        }
        return value;
    }

    private <T extends Enum<T>> Optional<T> symbol(InvocationExpressionSyntax call, int index, Class<T> type,
            String source, List<WardrobeDiagnostic> diagnostics) {
        Optional<String> raw = WearableCallSyntax.positionalSymbol(call, index);
        try {
            return raw.map(value -> Enum.valueOf(type, value));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(error(type.getSimpleName() + " is not supported: " + raw.orElse("?"), source));
            return Optional.empty();
        }
    }

    private WardrobeDiagnostic error(String message, String source) {
        return WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_PROFILE_INVALID, message, source);
    }
}
