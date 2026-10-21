package io.github.kxyr0.echonpc.forge.resource;

import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Discovers UTF-8 NPC and form server-data resources under Echo NPC folders.
 */
public final class NpcResourceDiscovery {
    public DiscoveredResources discoverAll(ResourceManager manager, DiagnosticSink diagnostics) {
        return new DiscoveredResources(discover(manager, diagnostics), discoverForms(manager, diagnostics));
    }

    public List<NpcResourceCandidate> discover(ResourceManager manager, DiagnosticSink diagnostics) {
        List<NpcResourceCandidate> candidates = new ArrayList<>();
        manager.listResources("echo_npc/npcs", this::isNpcSource).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), candidates, diagnostics));
        return List.copyOf(candidates);
    }

    public List<NpcResourceCandidate> discoverForms(ResourceManager manager, DiagnosticSink diagnostics) {
        List<NpcResourceCandidate> candidates = new ArrayList<>();
        manager.listResources("echo_npc/forms", this::isFormSource).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), candidates, diagnostics));
        return List.copyOf(candidates);
    }

    private boolean isNpcSource(ResourceLocation location) {
        return location.getPath().endsWith(".npc.es");
    }

    private boolean isFormSource(ResourceLocation location) {
        return location.getPath().endsWith(".form.es") || location.getPath().endsWith(".es");
    }

    private void readOne(ResourceLocation location, Resource resource, List<NpcResourceCandidate> candidates,
            DiagnosticSink diagnostics) {
        try (var stream = resource.open()) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            candidates.add(new NpcResourceCandidate(location, text));
        } catch (IOException exception) {
            diagnostics.accept(NpcDiagnostic.error("ENPC-RESOURCE-READ", "Не удалось прочитать NPC resource.",
                    location + ": " + exception.getClass().getSimpleName()));
        }
    }

    public record DiscoveredResources(
            List<NpcResourceCandidate> npcs,
            List<NpcResourceCandidate> forms
    ) {
        public DiscoveredResources {
            npcs = List.copyOf(java.util.Objects.requireNonNull(npcs, "npcs"));
            forms = List.copyOf(java.util.Objects.requireNonNull(forms, "forms"));
        }
    }
}
