package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNodeKind;
import java.util.Optional;
import java.util.Set;

/**
 * Bound node fields before they are converted into neutral source DTOs.
 */
record NodeSpec(
        SourceNodeKind kind,
        SourceSpan span,
        Optional<String> next,
        Optional<String> variable,
        Optional<String> value,
        Optional<String> expected,
        Optional<String> actionType,
        Set<String> fields) {
    static NodeSpec start(SourceSpan span, Optional<String> next) {
        return new NodeSpec(
                SourceNodeKind.START,
                span,
                next,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                fields(SourceNodeKind.START));
    }

    static NodeSpec set(
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
                fields(SourceNodeKind.SET_VARIABLE));
    }

    static NodeSpec require(
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
                fields(SourceNodeKind.CONDITION));
    }

    static NodeSpec action(SourceSpan span, Optional<String> next, Optional<String> actionType) {
        return new NodeSpec(
                SourceNodeKind.EXTERNAL_ACTION,
                span,
                next,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                actionType,
                fields(SourceNodeKind.EXTERNAL_ACTION));
    }

    static NodeSpec finish(SourceSpan span) {
        return new NodeSpec(
                SourceNodeKind.FINISH,
                span,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
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
}
