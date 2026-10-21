package io.github.kxyr0.echoscene.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.compile.SceneCompileResult;
import io.github.kxyr0.echoscene.compile.SceneSymbol;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.SceneSymbolKind;
import io.github.kxyr0.echoscene.compile.pipeline.SceneCompiler;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import io.github.kxyr0.echoscene.forge.command.MovementCommandAdapter.SceneLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

final class SceneForgeCatalog {
    private final AtomicLong revision = new AtomicLong();
    private volatile Map<SceneResourceId, SceneCompileResult> entries = Map.of();
    private volatile Map<String, SceneLocation> locations = Map.of();

    SimplePreparableReloadListener<Snapshot> listener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<String, SceneLocation> loadedLocations = loadLocations(manager);
                return new Snapshot(compile(manager, loadedLocations), loadedLocations);
            }

            @Override
            protected void apply(Snapshot value, ResourceManager manager, ProfilerFiller profiler) {
                entries = value.scenes();
                locations = value.locations();
            }
        };
    }

    List<SceneCompileResult> list() {
        return entries.values().stream().toList();
    }

    Optional<SceneCompileResult> find(String id) {
        try {
            return Optional.ofNullable(entries.get(new SceneResourceId(id)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    Optional<SceneLocation> location(String id) { return Optional.ofNullable(locations.get(id)); }
    List<SceneLocation> audioLocations(String reference) {
        return locations.values().stream().filter(location -> location.id().equals(reference)
                || symbolName(location.id()).equals(reference)).toList();
    }

    private Map<SceneResourceId, SceneCompileResult> compile(ResourceManager manager,
            Map<String, SceneLocation> loadedLocations) {
        SceneCompiler compiler = new SceneCompiler(SceneSymbolCatalog.of(symbols(manager, loadedLocations)));
        Map<SceneResourceId, SceneCompileResult> compiled = new LinkedHashMap<>();
        manager.listResources("echo_scene/scenes", id -> id.getPath().endsWith(".es"))
                .forEach((id, resource) -> compileOne(compiler, id, resource, compiled));
        return compiled;
    }

    private void compileOne(SceneCompiler compiler, ResourceLocation id,
            net.minecraft.server.packs.resources.Resource resource,
            Map<SceneResourceId, SceneCompileResult> target) {
        try (var reader = resource.openAsReader()) {
            String text = reader.lines().collect(java.util.stream.Collectors.joining("\n")) + "\n";
            String source = "data/" + id.getNamespace() + "/" + id.getPath();
            SceneCompileResult result = compiler.compile(new SceneSourceInput(id.getNamespace(), source, text,
                    revision.incrementAndGet()));
            SceneResourceId resourceId = result.definition().map(SceneDefinition::resourceId)
                    .orElseGet(() -> inferred(id));
            target.put(resourceId, result);
        } catch (IOException exception) {
            throw new IllegalStateException("SCENE_COMPILE_FAILED: " + id, exception);
        }
    }

    private List<SceneSymbol> symbols(ResourceManager manager, Map<String, SceneLocation> loadedLocations) {
        List<SceneSymbol> symbols = new ArrayList<>();
        symbols.add(new SceneSymbol("John", "jdh:john", SceneSymbolKind.PLAYER));
        loadedLocations.keySet().stream().map(id -> new SceneSymbol(symbolName(id), id, SceneSymbolKind.LOCATION))
                .forEach(symbols::add);
        for (String prefix : List.of("echo_npc/npcs", "echo_dialogue/dialogues",
                "echo_animation/animations", "echo_scene/scenes")) {
            manager.listResources(prefix, id -> id.getPath().endsWith(".es")).keySet().stream()
                    .map(this::symbol).flatMap(Optional::stream).forEach(symbols::add);
        }
        manager.listResources("echo_scene/cameras", id -> id.getPath().endsWith(".json")).keySet().stream()
                .map(id -> resourceId(id, ".json")).map(id -> new SceneSymbol(symbolName(id), id,
                        SceneSymbolKind.CAMERA)).forEach(symbols::add);
        return List.copyOf(symbols);
    }

    private Optional<SceneSymbol> symbol(ResourceLocation id) {
        SceneSymbolKind kind = kind(id.getPath());
        if (kind == null) return Optional.empty();
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1).replaceFirst("\\..*$", "");
        String name = java.util.Arrays.stream(file.split("[_-]+"))
                .map(value -> value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1))
                .collect(java.util.stream.Collectors.joining());
        return Optional.of(new SceneSymbol(name, id.getNamespace() + ":" + file, kind));
    }

    private SceneSymbolKind kind(String path) {
        if (path.contains("echo_npc/npcs/")) return SceneSymbolKind.NPC;
        if (path.contains("echo_dialogue/dialogues/")) return SceneSymbolKind.DIALOGUE;
        if (path.contains("echo_animation/animations/")) return SceneSymbolKind.ANIMATION;
        if (path.contains("echo_scene/scenes/")) return SceneSymbolKind.SCENE;
        return null;
    }

    private SceneResourceId inferred(ResourceLocation id) {
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1).replaceFirst("(?i)(\\.scene)?\\.es$", "");
        return new SceneResourceId(id.getNamespace() + ":" + file);
    }

    private Map<String, SceneLocation> loadLocations(ResourceManager manager) {
        Map<String, SceneLocation> loaded = new LinkedHashMap<>();
        manager.listResources("echo_scene/locations", id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (var reader = resource.openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        String resourceId = json.has("id") ? json.get("id").getAsString() : resourceId(id, ".json");
                        loaded.put(resourceId, new SceneLocation(resourceId, json.get("dimension").getAsString(),
                                json.get("x").getAsDouble(), json.get("y").getAsDouble(),
                                json.get("z").getAsDouble()));
                    } catch (IOException | RuntimeException exception) {
                        throw new IllegalStateException("SCENE_LOCATION_INVALID: " + id, exception);
                    }
                });
        return Map.copyOf(loaded);
    }

    private String resourceId(ResourceLocation id, String suffix) {
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        return id.getNamespace() + ":" + file.substring(0, file.length() - suffix.length());
    }

    private String symbolName(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        String file = path.substring(path.lastIndexOf('/') + 1);
        return java.util.Arrays.stream(file.split("[_-]+"))
                .map(value -> value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1))
                .collect(java.util.stream.Collectors.joining());
    }

    private record Snapshot(Map<SceneResourceId, SceneCompileResult> scenes,
                            Map<String, SceneLocation> locations) {
        private Snapshot {
            scenes = Map.copyOf(scenes);
            locations = Map.copyOf(locations);
        }
    }
}
