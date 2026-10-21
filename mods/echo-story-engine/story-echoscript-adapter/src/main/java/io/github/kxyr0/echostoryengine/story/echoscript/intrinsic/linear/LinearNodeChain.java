package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.linear;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LinearNodeChain {
    private final SourceNodeFactory nodes;

    public LinearNodeChain(SourceNodeFactory nodes) { this.nodes = nodes; }

    public List<SourceNode> link(SourceSpan span, String startNode, List<Entry> input) {
        Set<String> used = new LinkedHashSet<>(Set.of(startNode));
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Entry> entries = input.stream()
                .map(value -> new Entry(unique(value.id(), used, counts), value.spec())).toList();
        String finish = unique("finish", used, counts);
        List<SourceNode> output = new ArrayList<>();
        String first = entries.isEmpty() ? finish : entries.get(0).id();
        output.add(nodes.sourceNode(startNode, NodeSpec.start(span, Optional.of(first))));
        for (int index = 0; index < entries.size(); index++) {
            Entry value = entries.get(index);
            String next = index + 1 == entries.size() ? finish : entries.get(index + 1).id();
            output.add(nodes.sourceNode(value.id(), next(value.spec(), next)));
        }
        output.add(nodes.sourceNode(finish, NodeSpec.finish(span)));
        return List.copyOf(output);
    }

    private NodeSpec next(NodeSpec spec, String next) {
        return new NodeSpec(spec.kind(), spec.span(), Optional.of(next), spec.variable(), spec.value(),
                spec.expected(), spec.actionType(), spec.output(), spec.arguments(), spec.fields());
    }

    private String unique(String base, Set<String> used, Map<String, Integer> counts) {
        int count = counts.merge(base, 1, Integer::sum);
        String candidate = count == 1 ? base : base + "_" + count;
        while (!used.add(candidate)) {
            count = counts.merge(base, 1, Integer::sum);
            candidate = base + "_" + count;
        }
        return candidate;
    }

    public record Entry(String id, NodeSpec spec) { }
}
