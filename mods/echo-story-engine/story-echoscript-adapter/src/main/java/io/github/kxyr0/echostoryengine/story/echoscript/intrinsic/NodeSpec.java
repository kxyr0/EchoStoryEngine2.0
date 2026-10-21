package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNodeKind;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bound node fields before they are converted into neutral source DTOs.
 */
public record NodeSpec(
        SourceNodeKind kind,
        SourceSpan span,
        Optional<String> next,
        Optional<String> variable,
        Optional<String> value,
        Optional<String> expected,
        Optional<String> actionType,
        Optional<String> output,
        Map<String, String> arguments,
        Set<String> fields) {
    public static NodeSpec start(SourceSpan span, Optional<String> next) {
        return new NodeSpec(
                SourceNodeKind.START,
                span,
                next,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                fields(SourceNodeKind.START));
    }

    public static NodeSpec set(
            SourceSpan span,
            Optional<String> next,
            Optional<String> variable,
            Optional<String> value) {
        return new NodeSpec(
                SourceNodeKind.SET_VARIABLE,
                span,
                next,
                variable,
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                fields(SourceNodeKind.SET_VARIABLE));
    }

    public static NodeSpec require(
            SourceSpan span,
            Optional<String> next,
            Optional<String> variable,
            Optional<String> expected) {
        return new NodeSpec(
                SourceNodeKind.CONDITION,
                span,
                next,
                variable,
                Optional.empty(),
                expected,
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                fields(SourceNodeKind.CONDITION));
    }

    public static NodeSpec action(
            SourceSpan span,
            ActionFields action) {
        return new NodeSpec(
                SourceNodeKind.EXTERNAL_ACTION,
                span,
                action.next(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                action.actionType(),
                action.output(),
                Map.copyOf(action.arguments()),
                actionFields(action.output(), action.arguments()));
    }

    public static NodeSpec finish(SourceSpan span) {
        return new NodeSpec(
                SourceNodeKind.FINISH,
                span,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                fields(SourceNodeKind.FINISH));
    }

    private static Set<String> fields(SourceNodeKind kind) {
        return switch (kind) {
            case START -> Set.of("id", "kind", "next");
            case SET_VARIABLE -> Set.of("id", "kind", "variable", "value", "next");
            case CONDITION -> Set.of("id", "kind", "variable", "expectedValue", "next");
            case EXTERNAL_ACTION -> Set.of("id", "kind", "actionType", "next");
            case FINISH -> Set.of("id", "kind");
        };
    }

    private static Set<String> actionFields(Optional<String> output, Map<String, String> arguments) {
        if (output.isPresent() && !arguments.isEmpty()) {
            return Set.of("id", "kind", "actionType", "arguments", "output", "next");
        }
        if (output.isPresent()) {
            return Set.of("id", "kind", "actionType", "output", "next");
        }
        return arguments.isEmpty()
                ? fields(SourceNodeKind.EXTERNAL_ACTION)
                : Set.of("id", "kind", "actionType", "arguments", "next");
    }

    public record ActionFields(
            Optional<String> next,
            Optional<String> actionType,
            Optional<String> output,
            Map<String, String> arguments) {
    }
}
