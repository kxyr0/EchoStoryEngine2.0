package io.github.kxyr0.echomagic.api.projectile;

import java.util.Objects;

public record SpellCollisionDefinition(SpellBlockCollision blocks, SpellEntityCollision entities) {
    public SpellCollisionDefinition {
        blocks = Objects.requireNonNull(blocks, "blocks");
        entities = Objects.requireNonNull(entities, "entities");
    }
}

