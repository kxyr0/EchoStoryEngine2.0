package io.github.kxyr0.echoquests.forge.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.packs.resources.ResourceManager;

final class QuestResourceDiscovery {
    List<QuestResourceText> discover(ResourceManager resources) {
        List<QuestResourceText> values = new ArrayList<>();
        resources.listResources("echo_quests/quests", id -> id.getPath().endsWith(".quest.es"))
                .entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> values.add(new QuestResourceText(entry.getKey().toString(), read(entry.getValue()))));
        return List.copyOf(values);
    }

    private String read(net.minecraft.server.packs.resources.Resource resource) {
        try (var stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read quest resource", exception);
        }
    }
}
