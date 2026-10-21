package io.github.kxyr0.echostoryengine.story.forge.resource.discovery;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Validates the maximum number of story scripts in one resource folder.
 */
final class StoryResourceFolderLimit {
    private final int limit;

    StoryResourceFolderLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    List<StoryDiagnostic> validate(
            Collection<ResourceLocation> locations,
            ForgeRuntimeDiagnostics diagnostics,
            Instant occurredAt) {
        Map<String, Integer> counts = new TreeMap<>();
        for (ResourceLocation location : locations) {
            counts.merge(folderId(location), 1, Integer::sum);
        }
        List<StoryDiagnostic> values = new ArrayList<>();
        counts.forEach((folderId, count) -> {
            if (count > limit) {
                values.add(diagnostics.scriptFolderLimitExceeded(folderId, count, limit, occurredAt));
            }
        });
        return List.copyOf(values);
    }

    private String folderId(ResourceLocation location) {
        return location.getNamespace() + ":" + parentPath(location.getPath());
    }

    private String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator < 0 ? "" : path.substring(0, separator);
    }
}
