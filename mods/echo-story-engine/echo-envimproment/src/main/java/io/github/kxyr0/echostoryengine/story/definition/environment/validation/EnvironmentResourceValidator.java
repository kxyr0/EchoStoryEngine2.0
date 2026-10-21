package io.github.kxyr0.echostoryengine.story.definition.environment.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs loader-neutral EnvironmentResource validation.
 */
public final class EnvironmentResourceValidator {
    private final EnvironmentDiagnosticFactory diagnostics = new EnvironmentDiagnosticFactory();

    public EnvironmentValidationResult validate(
            EnvironmentResourceDef resource,
            String sourceId,
            Instant occurredAt) {
        List<StoryDiagnostic> values = new ArrayList<>();
        ValidationContext context = new ValidationContext(sourceId, occurredAt, values);
        if (resource.states().isEmpty()) {
            addResourceError(new ErrorSeed(
                    "ESE-ENV-SOURCE-001",
                    "Environment must contain at least one state.",
                    Map.of("environmentId", resource.id().asString())), resource, context);
        }
        validateStates(resource, context);
        return new EnvironmentValidationResult(values);
    }

    private void validateStates(
            EnvironmentResourceDef resource,
            ValidationContext context) {
        Set<String> stateIds = new HashSet<>();
        for (EnvironmentStateDef state : resource.states()) {
            if (!stateIds.add(state.id())) {
                addStateError(new ErrorSeed(
                        "ESE-ENV-SOURCE-002",
                        "Environment declares duplicate state id '" + state.id() + "'.",
                        Map.of("stateId", state.id())), state, context);
            }
            validateChanges(state, context);
        }
    }

    private void validateChanges(
            EnvironmentStateDef state,
            ValidationContext context) {
        Set<EnvironmentPosition> positions = new HashSet<>();
        for (EnvironmentChangeDef change : state.changes()) {
            if (!positions.add(change.position())) {
                context.values().add(diagnostics.error("ESE-ENV-SOURCE-003",
                        "Environment state '" + state.id() + "' modifies position "
                                + change.position().asString() + " more than once.",
                        new EnvironmentDiagnosticFactory.DiagnosticInput(
                                context.sourceId(), change.location(), context.occurredAt()),
                        Map.of("stateId", state.id(), "position", change.position().asString())));
            }
        }
    }

    private void addResourceError(
            ErrorSeed seed,
            EnvironmentResourceDef resource,
            ValidationContext context) {
        context.values().add(diagnostics.error(seed.code(), seed.message(),
                new EnvironmentDiagnosticFactory.DiagnosticInput(
                        context.sourceId(), resource.location(), context.occurredAt()), seed.context()));
    }

    private void addStateError(
            ErrorSeed seed,
            EnvironmentStateDef state,
            ValidationContext context) {
        context.values().add(diagnostics.error(seed.code(), seed.message(),
                new EnvironmentDiagnosticFactory.DiagnosticInput(
                        context.sourceId(), state.location(), context.occurredAt()), seed.context()));
    }

    private record ErrorSeed(String code, String message, Map<String, String> context) {
    }

    private record ValidationContext(
            String sourceId,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
    }
}
