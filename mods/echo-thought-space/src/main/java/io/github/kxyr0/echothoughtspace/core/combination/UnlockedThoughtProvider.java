package io.github.kxyr0.echothoughtspace.core.combination;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UnlockedThoughtProvider implements ThoughtNodeProvider {
    private final Map<ThoughtNodeId, ThoughtNodeSnapshot> unlocked = new LinkedHashMap<>();
    private List<ThoughtNodeSnapshot> cached = List.of();
    private long revision;

    public boolean unlock(ThoughtCombinationRecipe recipe) {
        if (unlocked.containsKey(recipe.result())) return false;
        revision++;
        unlocked.put(recipe.result(), new ThoughtNodeSnapshot(recipe.result(), recipe.label(),
                new ThoughtNodeRevision(revision)));
        cached = List.copyOf(unlocked.values());
        return true;
    }

    public void clear() {
        if (unlocked.isEmpty()) return;
        unlocked.clear();
        cached = List.of();
        revision++;
    }

    public int size() { return unlocked.size(); }
    public boolean contains(ThoughtNodeId id) { return unlocked.containsKey(id); }
    @Override public String providerId() { return "echo_thought_space:unlocked_combinations"; }
    @Override public long revision() { return revision; }
    @Override public List<ThoughtNodeSnapshot> nodes() { return cached; }
}
