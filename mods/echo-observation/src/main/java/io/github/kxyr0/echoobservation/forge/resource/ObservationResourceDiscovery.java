package io.github.kxyr0.echoobservation.forge.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class ObservationResourceDiscovery {
    private static final String ROOT = "echo_observation/observations";

    List<ObservationResourceText> discover(ResourceManager resources) {
        Map<ResourceLocation, Resource> found = resources.listResources(ROOT,
                location -> location.getPath().endsWith(".es"));
        List<ObservationResourceText> result = new ArrayList<>();
        found.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> read(entry.getKey(), entry.getValue(), result));
        return List.copyOf(result);
    }

    private void read(ResourceLocation location, Resource resource, List<ObservationResourceText> output) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) text.append(buffer, 0, count);
            output.add(new ObservationResourceText(location.toString(), text.toString()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Observation resource " + location, exception);
        }
    }
}
