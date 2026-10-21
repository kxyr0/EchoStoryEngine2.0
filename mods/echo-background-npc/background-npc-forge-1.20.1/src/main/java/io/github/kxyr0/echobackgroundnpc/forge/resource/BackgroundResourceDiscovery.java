package io.github.kxyr0.echobackgroundnpc.forge.resource;

import io.github.kxyr0.echobackgroundnpc.echoscript.BackgroundScriptSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class BackgroundResourceDiscovery {
    public List<BackgroundScriptSource> discover(ResourceManager resources) {
        List<BackgroundScriptSource> result = new ArrayList<>();
        resources.listResources("echo_background_npc", value -> value.getPath().endsWith(".es"))
                .entrySet().stream().sorted(Comparator.comparing(value -> value.getKey().toString()))
                .forEach(value -> read(value.getKey(), value.getValue(), result));
        return List.copyOf(result);
    }

    private void read(ResourceLocation id, net.minecraft.server.packs.resources.Resource resource,
            List<BackgroundScriptSource> result) {
        try (var stream = resource.open()) {
            result.add(new BackgroundScriptSource(id.toString(),
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read background resource " + id, exception);
        }
    }
}
