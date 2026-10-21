package io.github.kxyr0.echotrigger.core.binding.callback;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackAction;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackDefinition;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackSignature;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ParameterSyntax;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TriggerCallbackBinder {
    public Map<TriggerCallbackKind, TriggerCallbackDefinition> bind(
            List<MethodSyntax> methods,
            List<TriggerDiagnostic> diagnostics
    ) {
        Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks = new EnumMap<>(TriggerCallbackKind.class);
        for (TriggerCallbackKind kind : TriggerCallbackKind.values()) {
            bindKind(kind, methods, diagnostics).ifPresent(definition -> callbacks.put(kind, definition));
        }
        return Map.copyOf(callbacks);
    }

    private Optional<TriggerCallbackDefinition> bindKind(
            TriggerCallbackKind kind,
            List<MethodSyntax> methods,
            List<TriggerDiagnostic> diagnostics
    ) {
        List<MethodSyntax> candidates = methods.stream()
                .filter(method -> method.name().equals(kind.methodName()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        List<SignedMethod> valid = validMethods(candidates, diagnostics);
        if (hasDuplicateSignature(valid)) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_CALLBACK_DUPLICATE,
                    kind.methodName() + " declares duplicate callback signatures."));
            return Optional.empty();
        }
        if (valid.size() > 1) {
            diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_CALLBACK_SIGNATURE_AMBIGUOUS,
                    kind.methodName() + " must not use overloads."));
            return Optional.empty();
        }
        return valid.stream().findFirst().map(method -> definition(kind, method));
    }

    private List<SignedMethod> validMethods(
            List<MethodSyntax> candidates,
            List<TriggerDiagnostic> diagnostics
    ) {
        List<SignedMethod> valid = new ArrayList<>();
        for (MethodSyntax method : candidates) {
            Optional<TriggerCallbackSignature> signature = signature(method.parameters());
            if (signature.isEmpty()) {
                diagnostics.add(diagnostic(TriggerDiagnosticCode.TRIGGER_CALLBACK_SIGNATURE_INVALID,
                        method.name() + " accepts only no args, PlayerRef, or TriggerEvent."));
            } else {
                valid.add(new SignedMethod(method, signature.orElseThrow()));
            }
        }
        return List.copyOf(valid);
    }

    private boolean hasDuplicateSignature(List<SignedMethod> methods) {
        return methods.stream().map(SignedMethod::signature).distinct().count() != methods.size();
    }

    private Optional<TriggerCallbackSignature> signature(List<ParameterSyntax> parameters) {
        if (parameters.isEmpty()) {
            return Optional.of(TriggerCallbackSignature.NONE);
        }
        if (parameters.size() != 1) {
            return Optional.empty();
        }
        return switch (parameters.get(0).type().name().text()) {
            case "PlayerRef" -> Optional.of(TriggerCallbackSignature.PLAYER_REF);
            case "TriggerEvent" -> Optional.of(TriggerCallbackSignature.TRIGGER_EVENT);
            default -> Optional.empty();
        };
    }

    private TriggerCallbackDefinition definition(TriggerCallbackKind kind, SignedMethod method) {
        return new TriggerCallbackDefinition(kind, method.method().name(), method.signature(),
                actions(method.method()));
    }

    private List<TriggerCallbackAction> actions(MethodSyntax method) {
        return method.body().statements().stream()
                .flatMap(statement -> TriggerCallSyntax.statementCall(statement).stream())
                .map(call -> new TriggerCallbackAction(TriggerCallSyntax.qualifiedName(call),
                        TriggerCallSyntax.argumentValues(call)))
                .toList();
    }

    private TriggerDiagnostic diagnostic(TriggerDiagnosticCode code, String message) {
        return new TriggerDiagnostic(code, message);
    }

    private record SignedMethod(MethodSyntax method, TriggerCallbackSignature signature) {
    }
}
