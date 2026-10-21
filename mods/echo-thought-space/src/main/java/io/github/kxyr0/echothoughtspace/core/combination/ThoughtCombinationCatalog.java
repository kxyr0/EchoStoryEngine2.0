package io.github.kxyr0.echothoughtspace.core.combination;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ThoughtCombinationCatalog {
    private static final ThoughtCombinationCatalog EMPTY = new ThoughtCombinationCatalog(List.of());
    private final Map<ThoughtCombinationKey, ThoughtCombinationRecipe> byInputs;

    public ThoughtCombinationCatalog(List<ThoughtCombinationRecipe> recipes) {
        Map<ThoughtCombinationKey, ThoughtCombinationRecipe> values = new LinkedHashMap<>();
        Map<ThoughtNodeId, String> labels = new LinkedHashMap<>();
        for (ThoughtCombinationRecipe recipe : List.copyOf(recipes)) {
            if (values.putIfAbsent(recipe.inputs(), recipe) != null) {
                throw new IllegalArgumentException("Duplicate thought combination: " + recipe.inputs());
            }
            String previous = labels.putIfAbsent(recipe.result(), recipe.label().value());
            if (previous != null && !previous.equals(recipe.label().value())) {
                throw new IllegalArgumentException("Conflicting labels for result " + recipe.result());
            }
        }
        byInputs = Map.copyOf(values);
    }

    public Optional<ThoughtCombinationRecipe> find(ThoughtNodeId first, ThoughtNodeId second) {
        if (first.equals(second)) return Optional.empty();
        return Optional.ofNullable(byInputs.get(new ThoughtCombinationKey(first, second)));
    }

    public List<ThoughtCombinationRecipe> recipes() { return List.copyOf(byInputs.values()); }
    public int size() { return byInputs.size(); }
    public static ThoughtCombinationCatalog empty() { return EMPTY; }
}
