package io.github.kxyr0.echotrigger.core.binding.domain;

import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class EventActionBinder {
    public List<EventTriggerDefinition.Action> bind(BlockStatementSyntax body,
            Function<String, Optional<String>> aliases) {
        if (body.statements().isEmpty()) {
            throw new IllegalArgumentException("When требует хотя бы одно действие");
        }
        return body.statements().stream().map(statement -> action(TriggerCallSyntax.statementCall(statement)
                .orElseThrow(() -> new IllegalArgumentException("Реакция требует typed commands")), aliases)).toList();
    }

    private EventTriggerDefinition.Action action(InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases) {
        try {
            return definition(call, aliases);
        } catch (IllegalArgumentException failure) {
            var span = call.span();
            throw new EventBindingException(failure.getMessage(), new EventTriggerDefinition.Location(
                    span.sourceId().value(), span.start().line(), span.start().column()));
        }
    }

    private EventTriggerDefinition.Action definition(InvocationExpressionSyntax call,
            Function<String, Optional<String>> aliases) {
        var kind = switch (TriggerCallSyntax.qualifiedName(call)) {
            case "Scene.Play" -> EventTriggerDefinition.Kind.SCENE_PLAY;
            case "Story.Start" -> EventTriggerDefinition.Kind.STORY_START;
            case "Dialogue.Play" -> EventTriggerDefinition.Kind.DIALOGUE_PLAY;
            case "Observation.Activate" -> EventTriggerDefinition.Kind.OBSERVATION_ACTIVATE;
            case "Observation.Enable" -> EventTriggerDefinition.Kind.OBSERVATION_ENABLE;
            default -> throw new IllegalArgumentException("Неизвестная event action: "
                    + TriggerCallSyntax.qualifiedName(call));
        };
        if (call.arguments().size() != 1 || call.arguments().get(0).name().isPresent()) {
            throw new IllegalArgumentException("Action требует один ResourceId");
        }
        String target = TriggerCallSyntax.positionalString(call, 0)
                .or(() -> TriggerCallSyntax.positionalName(call, 0).flatMap(aliases))
                .orElseThrow(() -> new IllegalArgumentException("Action требует строковый ResourceId"));
        if ((kind == EventTriggerDefinition.Kind.OBSERVATION_ENABLE) != target.contains("#")) {
            throw new IllegalArgumentException("Observation.Enable требует #point; другие actions требуют resource ID");
        }
        var span = call.span();
        return new EventTriggerDefinition.Action(kind, new ResourceId(target), new EventTriggerDefinition.Location(
                span.sourceId().value(), span.start().line(), span.start().column()));
    }
}
