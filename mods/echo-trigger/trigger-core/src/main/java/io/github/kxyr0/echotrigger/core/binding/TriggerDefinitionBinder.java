package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackDefinition;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.callback.TriggerCallbackBinder;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.ReactiveTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerBoundArea;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echotrigger.core.definition.TriggerSequenceStep;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TriggerDefinitionBinder {
    private final TriggerMethodSequenceBinder sequenceBinder = new TriggerMethodSequenceBinder();
    private final TriggerCallbackBinder callbackBinder = new TriggerCallbackBinder();

    public TriggerCompileResult bind(CompilationUnitSyntax unit, String fingerprint, TriggerRevision revision) {
        List<TriggerDiagnostic> diagnostics = new ArrayList<>();
        validateBaseType(unit, diagnostics);
        TriggerFieldCatalog fields = TriggerFieldCatalog.from(unit.classDeclaration().members(), diagnostics);
        List<MethodSyntax> methods = methods(unit.classDeclaration().members());
        TriggerSemanticDiagnostics.rejectManualSleepCallbacks(methods, diagnostics);
        Optional<TriggerInitValues> values = readInit(methods, fields, diagnostics);
        boolean callbackMethods = hasCallbackMethods(methods);
        Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks = callbackBinder.bind(methods, diagnostics);
        values.ifPresent(init -> init.normalizeReactive(callbackMethods, diagnostics));
        List<TriggerSequenceStep> sequence = values
                .map(init -> sequence(init, methods, fields, diagnostics))
                .orElseGet(List::of);
        Optional<ReactiveTriggerDefinition> reactive = values
                .flatMap(init -> reactive(init, fields, callbacks, diagnostics));
        var eventSubscription = values.flatMap(init -> new
                io.github.kxyr0.echotrigger.core.binding.domain.EventTriggerBinder()
                .bind(methods, init.id().orElseThrow(), fingerprint, diagnostics, fields::string));
        if (eventSubscription.isEmpty()) {
            validateExecutable(values, sequence, callbackMethods, reactive, diagnostics);
        }
        if (!diagnostics.isEmpty() || values.isEmpty()) {
            return new TriggerCompileResult(Optional.empty(), diagnostics);
        }
        return new TriggerCompileResult(Optional.of(definition(values.orElseThrow(), sequence, reactive,
                new DefinitionStamp(fingerprint, revision, eventSubscription))), diagnostics);
    }

    private static CompiledTriggerDefinition definition(
            TriggerInitValues values,
            List<TriggerSequenceStep> sequence,
            Optional<ReactiveTriggerDefinition> reactive,
            DefinitionStamp stamp) {
        return new CompiledTriggerDefinition(values.id().orElseThrow(), values.scope(), values.mode(), stamp.revision(),
                values.cooldownTicks(), values.manualFire(), sequence, reactive,
                values.storySubscription(), stamp.fingerprint(), stamp.events());
    }

    private Optional<TriggerInitValues> readInit(
            List<MethodSyntax> methods,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        Optional<MethodSyntax> init = findMethod(methods, "Init");
        if (init.isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.MISSING_START,
                    "TriggerResource must declare Init()."));
            return Optional.empty();
        }
        return TriggerInitValues.read(init.orElseThrow(), fields, diagnostics);
    }

    private List<TriggerSequenceStep> sequence(
            TriggerInitValues values,
            List<MethodSyntax> methods,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        if (values.startMethod().isEmpty()) {
            return List.of();
        }
        Optional<MethodSyntax> sequenceMethod = values.startMethod().flatMap(name -> findMethod(methods, name));
        if (sequenceMethod.isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.MISSING_START,
                    "Start(...) points to a missing method."));
            return List.of();
        }
        return sequenceBinder.bind(sequenceMethod.orElseThrow(), fields, diagnostics);
    }

    private Optional<ReactiveTriggerDefinition> reactive(
            TriggerInitValues values,
            TriggerFieldCatalog fields,
            Map<TriggerCallbackKind, TriggerCallbackDefinition> callbacks,
            List<TriggerDiagnostic> diagnostics) {
        if (callbacks.isEmpty()) {
            return Optional.empty();
        }
        Optional<TriggerBoundArea> area = boundArea(values, fields, diagnostics);
        return area.map(bound -> new ReactiveTriggerDefinition(bound, callbacks, values.firePolicy(),
                values.stayIntervalTicks(), values.stayPolicy()));
    }

    private Optional<TriggerBoundArea> boundArea(
            TriggerInitValues values,
            TriggerFieldCatalog fields,
            List<TriggerDiagnostic> diagnostics) {
        if (values.bindCount() > 1) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_MULTIPLE_PRIMARY_AREAS,
                    "Reactive trigger may Bind exactly one primary area."));
        }
        if (values.boundAreaName().isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_AREA_NOT_BOUND,
                    "Reactive trigger requires Bind(AreaName)."));
            return Optional.empty();
        }
        if (values.dimension().isEmpty()) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.TRIGGER_AREA_DIMENSION_MISSING,
                    "Reactive trigger requires Dimension(\"namespace:id\")."));
            return Optional.empty();
        }
        String name = values.boundAreaName().orElseThrow();
        return fields.shape(name).map(shape -> new TriggerBoundArea(name, values.dimension().orElseThrow(), shape))
                .or(() -> missingShape(name, diagnostics));
    }

    private Optional<TriggerBoundArea> missingShape(String name, List<TriggerDiagnostic> diagnostics) {
        diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                TriggerDiagnosticCode.TRIGGER_AREA_SHAPE_INVALID,
                "Bind(" + name + ") must reference BoxArea, SphereArea, CylinderArea, CircleArea, or PolygonArea."));
        return Optional.empty();
    }

    private void validateExecutable(
            Optional<TriggerInitValues> values,
            List<TriggerSequenceStep> sequence,
            boolean callbackMethods,
            Optional<ReactiveTriggerDefinition> reactive,
            List<TriggerDiagnostic> diagnostics) {
        boolean storySubscription = values.map(TriggerInitValues::hasStorySubscription).orElse(false);
        if (values.isPresent() && sequence.isEmpty() && !callbackMethods && !storySubscription) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.MISSING_START,
                    "TriggerResource must declare Start(method), reactive callbacks, or Subscribe + StartStory."));
        }
        if (values.isPresent() && sequence.isEmpty() && reactive.isEmpty() && !storySubscription) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.INVALID_DEFINITION,
                    "TriggerResource has no executable sequence, subscription, or valid reactive callbacks."));
        }
    }

    private static void validateBaseType(CompilationUnitSyntax unit, List<TriggerDiagnostic> diagnostics) {
        if (!unit.classDeclaration().baseType().name().text().equals("TriggerResource")) {
            diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                    TriggerDiagnosticCode.INVALID_DEFINITION,
                    "Source must contain class ... : TriggerResource."));
        }
    }

    private static boolean hasCallbackMethods(List<MethodSyntax> methods) {
        return methods.stream().anyMatch(method -> List.of(TriggerCallbackKind.values()).stream()
                .anyMatch(kind -> kind.methodName().equals(method.name())));
    }

    private static List<MethodSyntax> methods(List<MemberSyntax> members) {
        return members.stream().filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast).toList();
    }

    private static Optional<MethodSyntax> findMethod(List<MethodSyntax> methods, String name) {
        return methods.stream().filter(method -> method.name().equals(name)).findFirst();
    }
    private record DefinitionStamp(String fingerprint, TriggerRevision revision,
            Optional<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition> events) { }
}
