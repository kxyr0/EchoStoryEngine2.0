package io.github.kxyr0.echothoughtspace.core.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ThoughtCombinationCatalogTest {
    @Test
    void lookupIsOrderIndependentAndUnlockIsIdempotent() {
        ThoughtCombinationRecipe recipe = recipe("a", "b", "c", "Conclusion");
        ThoughtCombinationCatalog catalog = new ThoughtCombinationCatalog(List.of(recipe));
        assertEquals(recipe, catalog.find(id("b"), id("a")).orElseThrow());
        UnlockedThoughtProvider unlocked = new UnlockedThoughtProvider();
        assertTrue(unlocked.unlock(recipe));
        assertFalse(unlocked.unlock(recipe));
        assertEquals(1, unlocked.nodes().size());
        assertEquals(id("c"), unlocked.nodes().get(0).id());
    }

    @Test
    void duplicatePairIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ThoughtCombinationCatalog(List.of(
                recipe("a", "b", "c", "One"), recipe("b", "a", "d", "Two"))));
    }

    private static ThoughtCombinationRecipe recipe(String left, String right, String result, String text) {
        return new ThoughtCombinationRecipe(new ThoughtCombinationKey(id(left), id(right)),
                id(result), new ThoughtNodeLabel(text));
    }

    private static ThoughtNodeId id(String value) { return new ThoughtNodeId(value); }
}
