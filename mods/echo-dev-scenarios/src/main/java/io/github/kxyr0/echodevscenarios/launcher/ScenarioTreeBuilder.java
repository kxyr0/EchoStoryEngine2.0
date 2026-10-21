package io.github.kxyr0.echodevscenarios.launcher;

import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioTreeBuilder {
    public ScenarioTreeNode build(List<ScenarioDefinition> definitions) {
        MutableNode root = new MutableNode("root");
        for (ScenarioDefinition definition : definitions) {
            MutableNode cursor = root;
            for (String group : definition.groups()) {
                cursor = cursor.children.computeIfAbsent(group, MutableNode::new);
            }
            cursor.scenarios.add(definition);
        }
        return freeze(root);
    }

    private ScenarioTreeNode freeze(MutableNode node) {
        return new ScenarioTreeNode(node.label, node.children.values().stream().map(this::freeze).toList(),
                node.scenarios.stream().sorted((a, b) -> a.id().compareTo(b.id())).toList());
    }

    private static final class MutableNode {
        private final String label;
        private final Map<String, MutableNode> children = new LinkedHashMap<>();
        private final List<ScenarioDefinition> scenarios = new ArrayList<>();
        private MutableNode(String label) { this.label = label; }
    }
}
