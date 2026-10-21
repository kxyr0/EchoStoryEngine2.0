package io.github.kxyr0.echodevscenarios.launcher;

import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import java.util.List;

public record ScenarioTreeNode(String label, List<ScenarioTreeNode> children, List<ScenarioDefinition> scenarios) {
    public ScenarioTreeNode {
        children = List.copyOf(children);
        scenarios = List.copyOf(scenarios);
    }
}
