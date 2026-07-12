package io.github.kxyr0.echostoryengine.story.definition.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticContext;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNodeKind;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates source shape before graph semantics or compilation.
 */
public final class StructuralStoryValidator {
    private static final int SUPPORTED_FORMAT_VERSION = 1;
    private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z][a-z0-9_.-]*:[a-z0-9_./-]+");
    private static final Pattern NODE_ID = Pattern.compile("[a-z][a-z0-9_.-]*");
    private final SourceDiagnosticCatalog diagnostics = new SourceDiagnosticCatalog();

    public StoryValidationResult validate(SourceStory story, Instant occurredAt) {
        List<StoryDiagnostic> values = new ArrayList<>();
        StructuralRun run = new StructuralRun(story, occurredAt, values);
        validateRoot(run);
        validateNodes(run);
        return new StoryValidationResult(story.sourceId(), story.formatVersion(), values);
    }

    private void validateRoot(StructuralRun run) {
        SourceStory story = run.story();
        if (story.formatVersion().isEmpty()) {
            run.values().add(missing(run, "formatVersion"));
        } else if (story.formatVersion().get() != SUPPORTED_FORMAT_VERSION) {
            run.values().add(storyDiagnostic("ESE-SOURCE-002", run,
                    Map.of("formatVersion", Integer.toString(story.formatVersion().get()))));
        }
        if (story.storyId().isEmpty()) {
            run.values().add(missing(run, "storyId"));
        } else if (!NAMESPACED_ID.matcher(story.storyId().get()).matches()) {
            run.values().add(storyDiagnostic("ESE-SOURCE-004", run,
                    Map.of("field", "storyId", "value", story.storyId().get())));
        }
        if (story.startNode().isEmpty()) {
            run.values().add(missing(run, "startNode"));
        } else if (!NODE_ID.matcher(story.startNode().get()).matches()) {
            run.values().add(storyDiagnostic("ESE-SOURCE-004", run,
                    Map.of("field", "startNode", "value", story.startNode().get())));
        }
        if (story.nodes().isEmpty()) {
            run.values().add(missing(run, "nodes"));
        }
    }

    private void validateNodes(StructuralRun run) {
        Set<String> ids = new HashSet<>();
        for (SourceNode node : run.story().nodes()) {
            validateNodeIdentity(run, node, ids);
            validateNodeKind(run, node);
            if (!node.invalidArguments().isEmpty()) {
                run.values().add(nodeDiagnostic("ESE-SOURCE-012", run, node,
                        Map.of("field", "arguments", "invalid", String.join(",", node.invalidArguments()))));
            }
        }
    }

    private void validateNodeIdentity(
            StructuralRun run,
            SourceNode node,
            Set<String> ids) {
        if (node.id().isEmpty()) {
            run.values().add(missing(run, node, "id"));
            return;
        }
        String id = node.id().get();
        if (!NODE_ID.matcher(id).matches()) {
            run.values().add(nodeDiagnostic("ESE-SOURCE-004", run, node,
                    Map.of("field", "id", "value", id)));
        } else if (!ids.add(id)) {
            run.values().add(nodeDiagnostic("ESE-SOURCE-005", run, node, Map.of("nodeId", id)));
        }
    }

    private void validateNodeKind(StructuralRun run, SourceNode node) {
        if (node.kind().isEmpty()) {
            run.values().add(missing(run, node, "kind"));
            return;
        }
        SourceNodeKind.from(node.kind().get())
                .ifPresentOrElse(
                        kind -> validateKindFields(run, node, kind),
                        () -> run.values().add(nodeDiagnostic("ESE-SOURCE-010", run, node,
                                Map.of("kind", node.kind().get()))));
    }

    private void validateKindFields(
            StructuralRun run,
            SourceNode node,
            SourceNodeKind kind) {
        requiredFields().get(kind).forEach(field -> requireField(run, node, field));
        Set<String> allowed = allowedFields().get(kind);
        node.fieldNames().stream()
                .filter(field -> !allowed.contains(field))
                .forEach(field -> run.values().add(nodeDiagnostic("ESE-SOURCE-012", run, node,
                        Map.of("field", field, "kind", kind.jsonName()))));
    }

    private void requireField(
            StructuralRun run,
            SourceNode node,
            String field) {
        if (!node.fieldNames().contains(field)) {
            run.values().add(missing(run, node, field));
        }
    }

    private Map<SourceNodeKind, Set<String>> requiredFields() {
        Map<SourceNodeKind, Set<String>> values = new EnumMap<>(SourceNodeKind.class);
        values.put(SourceNodeKind.START, Set.of("next"));
        values.put(SourceNodeKind.SET_VARIABLE, Set.of("variable", "value", "next"));
        values.put(SourceNodeKind.CONDITION, Set.of("variable", "expectedValue", "next"));
        values.put(SourceNodeKind.EXTERNAL_ACTION, Set.of("actionType", "next"));
        values.put(SourceNodeKind.FINISH, Set.of());
        return values;
    }

    private Map<SourceNodeKind, Set<String>> allowedFields() {
        Map<SourceNodeKind, Set<String>> values = new EnumMap<>(SourceNodeKind.class);
        values.put(SourceNodeKind.START, Set.of("id", "kind", "next"));
        values.put(SourceNodeKind.SET_VARIABLE, Set.of("id", "kind", "variable", "value", "next"));
        values.put(SourceNodeKind.CONDITION, Set.of("id", "kind", "variable", "expectedValue", "next"));
        values.put(SourceNodeKind.EXTERNAL_ACTION, Set.of("id", "kind", "actionType", "arguments", "next"));
        values.put(SourceNodeKind.FINISH, Set.of("id", "kind"));
        return values;
    }

    private StoryDiagnostic missing(StructuralRun run, String field) {
        return storyDiagnostic("ESE-SOURCE-003", run, Map.of("field", field));
    }

    private StoryDiagnostic missing(StructuralRun run, SourceNode node, String field) {
        return nodeDiagnostic("ESE-SOURCE-003", run, node, Map.of("field", field));
    }

    private StoryDiagnostic storyDiagnostic(
            String code,
            StructuralRun run,
            Map<String, String> context) {
        return diagnostics.create(code, run.occurredAt(), SourceDiagnosticContext.fromStory(run.story(), context));
    }

    private StoryDiagnostic nodeDiagnostic(
            String code,
            StructuralRun run,
            SourceNode node,
            Map<String, String> context) {
        return diagnostics.create(code, run.occurredAt(), SourceDiagnosticContext.fromNode(run.story(), node, context));
    }

    private record StructuralRun(
            SourceStory story,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
    }
}
