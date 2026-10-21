package io.github.kxyr0.echoobservation.echoscript.binding;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic.Severity;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import io.github.kxyr0.echoobservation.echoscript.ObservationCompileResult;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ObservationResourceBinder {
    private final ObservationDeclarationBinder declarations = new ObservationDeclarationBinder();

    public ObservationCompileResult bind(CompilationUnitSyntax unit, String sourceId, String source) {
        List<ObservationDiagnostic> diagnostics = new ArrayList<>();
        List<ClassSyntax> resources = unit.classDeclarations().stream()
                .filter(value -> value.baseType().name().text().equals("ObservationResource"))
                .toList();
        Optional<ObservationSourceContext> context = ObservationSourceContext.resolve(sourceId);
        if (resources.size() != 1 || context.isEmpty()) {
            error(diagnostics, 0, "Source must contain one ObservationResource and a namespaced file ID.");
            return new ObservationCompileResult(Optional.empty(), diagnostics);
        }
        ClassSyntax resource = resources.get(0);
        ObservationResourceId resourceId = resourceId(resource, context.orElseThrow(), diagnostics);
        long revision = sourceRevision(source);
        List<MethodSyntax> methods = resource.members().stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).filter(this::observationMethod).toList();
        if (methods.isEmpty()) {
            error(diagnostics, resource.span().startOffset(), "ObservationResource declares no Observation.");
        }
        List<ObservationDefinition> definitions = new ArrayList<>();
        methods.forEach(method -> definitions.add(declarations.bind(resourceId, method, sourceId,
                revision, diagnostics)));
        Set<Object> ids = new HashSet<>();
        definitions.stream().filter(value -> !ids.add(value.id())).forEach(value -> diagnostics.add(
                new ObservationDiagnostic(ObservationDiagnosticCode.OBSERVATION_ID_COLLISION, Severity.ERROR,
                        "Observation ID collision: " + value.id(), value.source().declarationStart())));
        if (diagnostics.stream().anyMatch(ObservationDiagnostic::error)) {
            return new ObservationCompileResult(Optional.empty(), diagnostics);
        }
        return new ObservationCompileResult(Optional.of(new ObservationResourceDefinition(resourceId,
                definitions, revision, sourceId)), diagnostics);
    }

    private ObservationResourceId resourceId(ClassSyntax resource, ObservationSourceContext context,
            List<ObservationDiagnostic> diagnostics) {
        List<ClassCallSyntax> ids = resource.members().stream().filter(ClassCallSyntax.class::isInstance)
                .map(ClassCallSyntax.class::cast)
                .filter(value -> ObservationCallReader.name(value.invocation()).equals("Id")).toList();
        if (ids.isEmpty()) return context.inferredId();
        if (ids.size() > 1) {
            error(diagnostics, ids.get(1).span().startOffset(), "ObservationResource declares Id more than once.");
            return context.inferredId();
        }
        try {
            return ObservationCallReader.positional(ids.get(0).invocation(), 0)
                    .flatMap(ObservationCallReader::string).map(ObservationResourceId::parse)
                    .orElseThrow();
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            error(diagnostics, ids.get(0).span().startOffset(), "Invalid Observation resource Id.");
            return context.inferredId();
        }
    }

    private boolean observationMethod(MethodSyntax method) {
        return method.returnType().name().text().equals("Observation") && method.parameters().isEmpty();
    }

    private long sourceRevision(String source) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < source.length(); index++) {
            hash = (hash ^ source.charAt(index)) * 0x100000001b3L;
        }
        return hash & Long.MAX_VALUE;
    }

    private void error(List<ObservationDiagnostic> diagnostics, int offset, String message) {
        diagnostics.add(new ObservationDiagnostic(ObservationDiagnosticCode.OBS_RESOURCE_INVALID,
                Severity.ERROR, message, offset));
    }
}
