package io.github.kxyr0.echocontentdoctor.validation.structural;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentNode;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReachabilityValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            if (descriptor.nodes().isEmpty()) continue;
            Map<String, ContentNode> nodes = new HashMap<>();
            descriptor.nodes().forEach(node -> nodes.put(node.id(), node));
            Set<String> reachable = traverse(nodes, descriptor.nodes().stream().filter(ContentNode::entry)
                    .map(ContentNode::id).toList());
            for (ContentNode node : descriptor.nodes()) {
                if (!reachable.contains(node.id()) && !node.externallyReachable()) diagnostics.add(diagnostic(descriptor, node));
            }
        }
        return diagnostics;
    }

    private Set<String> traverse(Map<String, ContentNode> nodes, List<String> entries) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>(entries);
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!visited.add(id)) continue;
            ContentNode node = nodes.get(id);
            if (node != null) node.edges().stream().filter(nodes::containsKey).forEach(queue::addLast);
        }
        return visited;
    }

    private ContentDiagnostic diagnostic(ContentDescriptor descriptor, ContentNode node) {
        String code = descriptor.kind() == ContentResourceKind.DIALOGUE
                ? "ECHO_DOCTOR_DIALOGUE_NODE_UNREACHABLE" : "ECHO_DOCTOR_SCENE_NODE_UNREACHABLE";
        return new ContentDiagnostic(ContentSeverity.WARNING, code, descriptor.id(), node.span(),
                "Node " + node.id() + " is unreachable from the entry.", List.of());
    }
}
