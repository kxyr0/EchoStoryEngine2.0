package io.github.kxyr0.echostoryengine.story.definition.validation;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticCatalog;
import io.github.kxyr0.echostoryengine.story.definition.diagnostic.SourceDiagnosticContext;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNodeKind;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates graph meaning without executing the story.
 */
public final class SemanticStoryValidator {
    private static final Pattern NAMESPACED_ID = Pattern.compile("[a-z][a-z0-9_.-]*:[a-z0-9_./-]+");
    private final SourceDiagnosticCatalog diagnostics = new SourceDiagnosticCatalog();

    public StoryValidationResult validate(SourceStory story, Instant occurredAt) {
        Map<String, SourceNode> nodes = nodeMap(story);
        List<StoryDiagnostic> values = new ArrayList<>();
        SemanticRun run = new SemanticRun(story, occurredAt, values);
        validateStart(run, nodes);
        validateTargets(run, nodes);
        validateActionTypes(run);
        validateReachability(run, nodes);
        validateSynchronousCycles(run, nodes);
        return new StoryValidationResult(story.sourceId(), story.formatVersion(), values);
    }

    private Map<String, SourceNode> nodeMap(SourceStory story) {
        Map<String, SourceNode> nodes = new HashMap<>();
        story.nodes().forEach(node -> node.id().ifPresent(id -> nodes.put(id, node)));
        return Map.copyOf(nodes);
    }

    private void validateStart(SemanticRun run, Map<String, SourceNode> nodes) {
        String start = run.story().startNode().orElse("");
        if (!nodes.containsKey(start)) {
            run.values().add(storyDiagnostic("ESE-SOURCE-006", run, Map.of("startNode", start)));
        }
    }

    private void validateTargets(SemanticRun run, Map<String, SourceNode> nodes) {
        run.story().nodes().forEach(node -> transitionTarget(node).ifPresent(target -> {
            if (!nodes.containsKey(target)) {
                run.values().add(nodeDiagnostic("ESE-SOURCE-007", run, node, Map.of("target", target)));
            }
        }));
    }

    private void validateActionTypes(SemanticRun run) {
        run.story().nodes().stream()
                .filter(node -> kind(node) == SourceNodeKind.EXTERNAL_ACTION)
                .forEach(node -> {
                    String actionType = node.actionType().orElse("");
                    if (!NAMESPACED_ID.matcher(actionType).matches()) {
                        run.values().add(nodeDiagnostic("ESE-SOURCE-004", run, node,
                                Map.of("field", "actionType", "value", actionType)));
                    }
                });
    }

    private void validateReachability(SemanticRun run, Map<String, SourceNode> nodes) {
        Set<String> reachable = reachableNodes(run.story(), nodes);
        if (reachable.stream()
                .map(nodes::get)
                .noneMatch(node -> node != null && kind(node) == SourceNodeKind.FINISH)) {
            run.values().add(storyDiagnostic("ESE-SOURCE-013", run, Map.of()));
        }
        nodes.keySet().stream()
                .filter(nodeId -> !reachable.contains(nodeId))
                .forEach(nodeId -> run.values().add(nodeDiagnostic("ESE-SOURCE-008", run, nodes.get(nodeId),
                        Map.of("nodeId", nodeId))));
    }

    private Set<String> reachableNodes(SourceStory story, Map<String, SourceNode> nodes) {
        Set<String> reachable = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        story.startNode().ifPresent(pending::add);
        while (!pending.isEmpty()) {
            String nodeId = pending.removeFirst();
            if (!reachable.add(nodeId) || !nodes.containsKey(nodeId)) {
                continue;
            }
            transitionTarget(nodes.get(nodeId)).ifPresent(pending::add);
        }
        return Set.copyOf(reachable);
    }

    private void validateSynchronousCycles(SemanticRun run, Map<String, SourceNode> nodes) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String nodeId : nodes.keySet()) {
            if (hasCycle(nodeId, nodes, visited, visiting)) {
                run.values().add(storyDiagnostic("ESE-SOURCE-009", run, Map.of("nodeId", nodeId)));
                return;
            }
        }
    }

    private boolean hasCycle(
            String nodeId,
            Map<String, SourceNode> nodes,
            Set<String> visited,
            Set<String> visiting) {
        if (visiting.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId) || !nodes.containsKey(nodeId) || !hasSynchronousEdge(nodes.get(nodeId))) {
            return false;
        }
        visiting.add(nodeId);
        boolean cycle = transitionTarget(nodes.get(nodeId))
                .filter(target -> hasCycle(target, nodes, visited, visiting))
                .isPresent();
        visiting.remove(nodeId);
        visited.add(nodeId);
        return cycle;
    }

    private boolean hasSynchronousEdge(SourceNode node) {
        SourceNodeKind kind = kind(node);
        return kind == SourceNodeKind.START || kind == SourceNodeKind.SET_VARIABLE || kind == SourceNodeKind.CONDITION;
    }

    private Optional<String> transitionTarget(SourceNode node) {
        return kind(node) == SourceNodeKind.FINISH ? Optional.empty() : node.nextNode();
    }

    private SourceNodeKind kind(SourceNode node) {
        return node.kind().flatMap(SourceNodeKind::from).orElse(SourceNodeKind.FINISH);
    }

    private StoryDiagnostic storyDiagnostic(
            String code,
            SemanticRun run,
            Map<String, String> context) {
        return diagnostics.create(code, run.occurredAt(), SourceDiagnosticContext.fromStory(run.story(), context));
    }

    private StoryDiagnostic nodeDiagnostic(
            String code,
            SemanticRun run,
            SourceNode node,
            Map<String, String> context) {
        return diagnostics.create(
                code,
                run.occurredAt(),
                SourceDiagnosticContext.fromNode(run.story(), node, context));
    }

    private record SemanticRun(
            SourceStory story,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
    }
}
