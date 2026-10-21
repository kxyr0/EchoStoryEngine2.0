package io.github.kxyr0.echostoryengine.story.forge.resource.discovery;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceText;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Discovers and reads Forge data resources for story source compilation.
 */
public final class StoryResourceDiscovery {
    private static final int MAX_SCRIPTS_PER_FOLDER = 7;
    private static final String STORY_RESOURCE_ROOT = "echo_story/stories";
    private static final String ENVIRONMENT_RESOURCE_ROOT = "echo_story/environments";

    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final StoryResourceFolderLimit folderLimit = new StoryResourceFolderLimit(MAX_SCRIPTS_PER_FOLDER);

    public StoryResourceDiscovery(StoryClock clock, ForgeRuntimeDiagnostics diagnostics) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public StoryResourceReadResult discover(ResourceManager resourceManager) {
        return discoverStories(resourceManager);
    }

    public StoryResourceReadResult discoverStories(ResourceManager resourceManager) {
        return discoverRoot(resourceManager, STORY_RESOURCE_ROOT);
    }

    public StoryResourceReadResult discoverEnvironments(ResourceManager resourceManager) {
        return discoverRoot(resourceManager, ENVIRONMENT_RESOURCE_ROOT);
    }

    private StoryResourceReadResult discoverRoot(ResourceManager resourceManager, String root) {
        Map<ResourceLocation, Resource> discovered = resourceManager.listResources(
                root,
                location -> location.getPath().endsWith(".es"));
        List<StoryDiagnostic> readDiagnostics = new ArrayList<>(
                folderLimit.validate(discovered.keySet(), diagnostics, clock.now()));
        List<StoryResourceText> resources = new ArrayList<>();
        discovered.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), resources, readDiagnostics));
        return new StoryResourceReadResult(resources, readDiagnostics);
    }

    private void readOne(
            ResourceLocation location,
            Resource resource,
            List<StoryResourceText> resources,
            List<StoryDiagnostic> readDiagnostics) {
        String sourceId = location.toString();
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            resources.add(new StoryResourceText(sourceId, readAll(reader)));
        } catch (IOException | RuntimeException exception) {
            readDiagnostics.add(diagnostics.resourceReadFailed(
                    sourceId,
                    exception.getClass().getSimpleName(),
                    clock.now()));
        }
    }

    private String readAll(InputStreamReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read = reader.read(buffer);
        while (read >= 0) {
            builder.append(buffer, 0, read);
            read = reader.read(buffer);
        }
        return builder.toString();
    }
}
