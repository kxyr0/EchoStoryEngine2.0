package io.github.kxyr0.echotrigger.forge.resource.inline;

import io.github.kxyr0.echotrigger.forge.resource.TriggerResourceText;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.resources.ResourceManager;

public final class InlineResourceDiscovery {
    public List<TriggerResourceText> discover(ResourceManager manager, List<TriggerResourceText> external)
            throws IOException {
        if (manager == null) {
            return List.of();
        }
        List<TriggerResourceText> resources = new ArrayList<>();
        for (String root : List.of("echo_dialogue/dialogues", "echo_story/stories", "echo_trigger/triggers")) {
            var found = manager.listResources(root, location -> location.getPath().endsWith(".es"));
            for (var entry : found.entrySet()) {
                var id = entry.getKey();
                String suffix = "/content-pack/" + id.getNamespace() + "/" + id.getPath();
                String dataSuffix = "/data/" + id.getNamespace() + "/" + id.getPath();
                if (external.stream().anyMatch(value -> {
                    String path = value.path().toString().replace('\\', '/');
                    return path.endsWith(suffix) || path.endsWith(dataSuffix);
                })) {
                    continue;
                }
                try (var input = entry.getValue().open()) {
                    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    if (text.contains("When") || root.equals("echo_trigger/triggers")) {
                        resources.add(new TriggerResourceText(Path.of("data", id.getNamespace(), id.getPath()), text));
                    }
                }
            }
        }
        return List.copyOf(resources);
    }
}
