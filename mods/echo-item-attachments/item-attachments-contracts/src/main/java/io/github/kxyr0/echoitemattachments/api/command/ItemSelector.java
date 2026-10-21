package io.github.kxyr0.echoitemattachments.api.command;

import java.util.Objects;
import java.util.Optional;

public record ItemSelector(Optional<String> itemId, int count, double radius) {
    public static final double MAX_RADIUS = 0.75D;

    public ItemSelector {
        itemId = Objects.requireNonNull(itemId, "item id").map(String::trim);
        if (itemId.isPresent() && !itemId.orElseThrow()
                .matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("item id must be namespaced");
        }
        if (count < 1) throw new IllegalArgumentException("count must be positive");
        if (!Double.isFinite(radius) || radius <= 0.0D || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("radius must be in (0, 0.75]");
        }
    }
}
