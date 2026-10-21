package io.github.kxyr0.echothoughtspace.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ThoughtNodeRepositoryTest {
    @Test
    void existingPositionsStayStableAndDuplicatesCollapse() {
        ThoughtNodeRepository repository = new ThoughtNodeRepository();
        MutableProvider provider = new MutableProvider(List.of(node("b", "B"), node("a", "A")));
        List<ThoughtCanvasNode> first = repository.refresh(List.of(provider));
        var aPosition = first.stream().filter(value -> value.snapshot().id().value().equals("a"))
                .findFirst().orElseThrow().position();
        provider.nodes = List.of(node("c", "C"), node("a", "A updated"), node("a", "duplicate"));
        provider.revision++;
        List<ThoughtCanvasNode> second = repository.refresh(List.of(provider));
        assertEquals(2, second.size());
        var updated = second.stream().filter(value -> value.snapshot().id().value().equals("a"))
                .findFirst().orElseThrow();
        assertEquals(aPosition, updated.position());
        assertEquals("A updated", updated.snapshot().label().value());
        assertNotEquals(aPosition, second.stream().filter(value -> value.snapshot().id().value().equals("c"))
                .findFirst().orElseThrow().position());
    }

    @Test
    void explicitPositionKeepsDerivedResultNearItsInputs() {
        ThoughtNodeRepository repository = new ThoughtNodeRepository();
        MutableProvider provider = new MutableProvider(List.of(node("a", "A"), node("result", "Result")));
        ThoughtCanvasPosition expected = new ThoughtCanvasPosition(75.0D, -110.0D);
        List<ThoughtCanvasNode> nodes = repository.refresh(List.of(provider),
                Map.of(new ThoughtNodeId("result"), expected));
        ThoughtCanvasNode result = nodes.stream().filter(node -> node.snapshot().id().value().equals("result"))
                .findFirst().orElseThrow();
        assertEquals(expected, result.position());
    }

    private static ThoughtNodeSnapshot node(String id, String label) {
        return new ThoughtNodeSnapshot(new ThoughtNodeId(id), new ThoughtNodeLabel(label),
                new ThoughtNodeRevision(1L));
    }

    private static final class MutableProvider implements ThoughtNodeProvider {
        private List<ThoughtNodeSnapshot> nodes;
        private long revision = 1L;
        private MutableProvider(List<ThoughtNodeSnapshot> nodes) { this.nodes = nodes; }
        @Override public String providerId() { return "test"; }
        @Override public long revision() { return revision; }
        @Override public List<ThoughtNodeSnapshot> nodes() { return nodes; }
    }
}
