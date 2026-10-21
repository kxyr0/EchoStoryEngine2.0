package io.github.kxyr0.echostoryengine.story.forge.environment.catalog;

import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import java.util.Objects;

/**
 * One compiled EnvironmentResource plus its source resource id.
 */
public record EnvironmentCatalogEntry(EnvironmentResourceDef resource, String sourceId) {
    public EnvironmentCatalogEntry {
        resource = Objects.requireNonNull(resource, "resource");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
    }
}
