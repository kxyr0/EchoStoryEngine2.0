package io.github.kxyr0.echothoughtspace.core.combination;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import java.util.Objects;

public record ThoughtCombinationKey(ThoughtNodeId first, ThoughtNodeId second) {
    public ThoughtCombinationKey {
        first = Objects.requireNonNull(first, "first");
        second = Objects.requireNonNull(second, "second");
        if (first.equals(second)) throw new IllegalArgumentException("A thought cannot combine with itself.");
        if (first.compareTo(second) > 0) {
            ThoughtNodeId swap = first;
            first = second;
            second = swap;
        }
    }
}
