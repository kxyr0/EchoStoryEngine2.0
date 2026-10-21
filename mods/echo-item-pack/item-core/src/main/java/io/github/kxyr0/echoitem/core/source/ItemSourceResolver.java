package io.github.kxyr0.echoitem.core.source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves packaged and external candidates in deterministic precedence order.
 */
public final class ItemSourceResolver {
    public List<ItemSourceCandidate> merge(
            List<ItemSourceCandidate> packaged,
            List<ItemSourceCandidate> external
    ) {
        List<ItemSourceCandidate> merged = new ArrayList<>();
        merged.addAll(packaged);
        merged.addAll(external);
        return merged.stream()
                .sorted(Comparator.comparing(ItemSourceCandidate::sourceId))
                .toList();
    }
}
