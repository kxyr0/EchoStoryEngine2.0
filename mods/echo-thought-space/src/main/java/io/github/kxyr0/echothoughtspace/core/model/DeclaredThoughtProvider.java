package io.github.kxyr0.echothoughtspace.core.model;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DeclaredThoughtProvider implements ThoughtNodeProvider {
    private List<ThoughtNodeSnapshot> nodes = List.of();
    private long revision;

    public void replace(List<ThoughtNodeSnapshot> replacements) {
        Map<ThoughtNodeId, ThoughtNodeSnapshot> unique = new LinkedHashMap<>();
        for (ThoughtNodeSnapshot node : List.copyOf(replacements)) {
            if (unique.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate declared thought ID: " + node.id());
            }
        }
        List<ThoughtNodeSnapshot> next = List.copyOf(unique.values());
        if (sameContent(nodes, next)) return;
        revision++;
        ThoughtNodeRevision nodeRevision = new ThoughtNodeRevision(revision);
        nodes = next.stream().map(node -> new ThoughtNodeSnapshot(node.id(), node.label(), nodeRevision)).toList();
    }

    private static boolean sameContent(List<ThoughtNodeSnapshot> first, List<ThoughtNodeSnapshot> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!first.get(index).id().equals(second.get(index).id())
                    || !first.get(index).label().equals(second.get(index).label())) return false;
        }
        return true;
    }

    @Override public String providerId() { return "echo_thought_space:declared_thoughts"; }
    @Override public long revision() { return revision; }
    @Override public List<ThoughtNodeSnapshot> nodes() { return nodes; }
}
