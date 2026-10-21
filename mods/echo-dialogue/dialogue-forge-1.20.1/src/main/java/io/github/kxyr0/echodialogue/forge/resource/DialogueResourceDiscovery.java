package io.github.kxyr0.echodialogue.forge.resource;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
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

/**
 * Discovers server data resources under echo_dialogue/dialogues.
 */
public final class DialogueResourceDiscovery {
    private static final String RESOURCE_ROOT = "echo_dialogue/dialogues";

    public DialogueResourceReadResult discover(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> discovered = resourceManager.listResources(
                RESOURCE_ROOT, location -> location.getPath().endsWith(".dialogue.es"));
        List<DialogueResourceText> resources = new ArrayList<>();
        List<DialogueDiagnostic> diagnostics = new ArrayList<>();
        discovered.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), resources, diagnostics));
        return new DialogueResourceReadResult(resources, diagnostics);
    }

    private void readOne(
            ResourceLocation location,
            Resource resource,
            List<DialogueResourceText> resources,
            List<DialogueDiagnostic> diagnostics
    ) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            resources.add(new DialogueResourceText(location.toString(), readAll(reader)));
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(DialogueDiagnostic.error("EDLG-FORGE-004", DiagnosticCategory.RESOURCE_DISCOVERY,
                    "Не удалось прочитать .dialogue.es ресурс: " + location));
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
