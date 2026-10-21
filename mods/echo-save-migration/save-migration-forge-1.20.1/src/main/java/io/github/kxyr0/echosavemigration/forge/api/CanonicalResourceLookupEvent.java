package io.github.kxyr0.echosavemigration.forge.api;

import io.github.kxyr0.echosavemigration.api.ResourceId;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Resolves resource renames using the existing migration catalog's AliasGraph. */
public final class CanonicalResourceLookupEvent extends Event {
    private final ResourceId requested;
    private ResourceId resolved;
    public CanonicalResourceLookupEvent(ResourceId requested) {
        this.requested = Objects.requireNonNull(requested, "requested");
    }
    public ResourceId requested() { return requested; }
    public ResourceId canonical() { return resolved == null ? requested : resolved; }
    public void resolve(ResourceId canonical) {
        if (resolved != null) throw new IllegalStateException("Multiple canonical resource providers");
        resolved = Objects.requireNonNull(canonical, "canonical");
    }
}
