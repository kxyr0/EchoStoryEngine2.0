package io.github.kxyr0.echodevscenarios.forge;

import com.google.gson.JsonParser;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCatalog;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCompiler;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbol;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbolKind;
import io.github.kxyr0.echodevscenarios.compile.source.ScenarioSourceInput;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.forge.setup.ScenarioLocation;
import io.github.kxyr0.echodevscenarios.forge.setup.ScenarioLocationCatalog;
import io.github.kxyr0.echodevscenarios.launcher.ScenarioCatalogStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

final class ForgeScenarioCatalog implements ScenarioLocationCatalog {
    private final ScenarioCatalogStore store = new ScenarioCatalogStore();
    private volatile Map<String, ScenarioLocation> locations = Map.of();

    ScenarioCatalogStore store() { return store; }
    @Override
    public Optional<ScenarioLocation> location(String id) { return Optional.ofNullable(locations.get(id)); }

    SimplePreparableReloadListener<Snapshot> listener() {
        return new SimplePreparableReloadListener<>() {
            protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) { return compile(manager); }
            protected void apply(Snapshot value, ResourceManager manager, ProfilerFiller profiler) {
                store.install(value.definitions(), value.errors());
                locations = Map.copyOf(value.locations());
            }
        };
    }

    private Snapshot compile(ResourceManager manager) {
        Map<String, ScenarioLocation> foundLocations = locations(manager);
        List<ScenarioSymbol> symbols = symbols(manager, foundLocations);
        Map<String, List<ScenarioTarget>> targets = targets(manager);
        ScenarioCatalog input = new ScenarioCatalog() {
            public List<ScenarioSymbol> symbols() { return symbols; }
            public List<ScenarioTarget> targets(String id) { return targets.getOrDefault(id, List.of()); }
        };
        ScenarioCompiler compiler = new ScenarioCompiler(input);
        List<ScenarioDefinition> definitions = new ArrayList<>();
        Map<ScenarioId, String> errors = new LinkedHashMap<>();
        manager.listResources("echo_dev_scenarios/dev/scenarios", id -> id.getPath().endsWith(".es"))
                .forEach((id, resource) -> compileOne(compiler, id, resource, definitions, errors));
        return new Snapshot(definitions, errors, foundLocations);
    }

    private void compileOne(ScenarioCompiler compiler, ResourceLocation id,
            net.minecraft.server.packs.resources.Resource resource, List<ScenarioDefinition> definitions,
            Map<ScenarioId, String> errors) {
        String path = id.getPath().substring(id.getPath().indexOf("dev/scenarios/"));
        ScenarioId scenarioId = inferred(path);
        try (var reader = resource.openAsReader()) {
            String text = reader.lines().collect(java.util.stream.Collectors.joining("\n")) + "\n";
            var result = compiler.compile(new ScenarioSourceInput(path, text));
            result.definition().ifPresent(definitions::add);
            if (!result.successful()) errors.put(scenarioId, result.diagnostics().stream()
                    .map(value -> value.message()).findFirst().orElse("DEV_SCENARIO_INVALID"));
        } catch (IOException exception) {
            errors.put(scenarioId, "DEV_SCENARIO_INVALID: " + exception.getMessage());
        }
    }

    private List<ScenarioSymbol> symbols(ResourceManager manager, Map<String, ScenarioLocation> foundLocations) {
        List<ScenarioSymbol> values = new ArrayList<>();
        addSymbols(manager, "echo_npc/npcs", ScenarioSymbolKind.NPC, values);
        addSymbols(manager, "echo_observation/observations", ScenarioSymbolKind.OBSERVATION, values);
        foundLocations.values().forEach(value -> values.add(new ScenarioSymbol(
                title(value.id().substring(value.id().indexOf(':') + 1)), value.id(), ScenarioSymbolKind.LOCATION)));
        return values;
    }

    private Map<String, ScenarioLocation> locations(ResourceManager manager) {
        Map<String, ScenarioLocation> values = new LinkedHashMap<>();
        manager.listResources("echo_scene/locations", id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (var reader = resource.openAsReader()) {
                        var json = JsonParser.parseReader(reader).getAsJsonObject();
                        String resourceId = id.getNamespace() + ":" + stem(id);
                        values.put(resourceId, new ScenarioLocation(resourceId, json.get("dimension").getAsString(),
                                json.get("x").getAsDouble(), json.get("y").getAsDouble(),
                                json.get("z").getAsDouble(), number(json, "yaw"), number(json, "pitch")));
                    } catch (IOException | RuntimeException exception) {
                        throw new IllegalStateException("DEV_SCENARIO_INVALID location " + id, exception);
                    }
                });
        return values;
    }

    private float number(com.google.gson.JsonObject json, String name) {
        return json.has(name) ? json.get(name).getAsFloat() : 0.0F;
    }

    private void addSymbols(ResourceManager manager, String prefix, ScenarioSymbolKind kind,
            List<ScenarioSymbol> target) {
        manager.listResources(prefix, id -> id.getPath().endsWith(".es")).keySet().forEach(id -> {
            String file = stem(id);
            target.add(new ScenarioSymbol(title(file), id.getNamespace() + ":" + file, kind));
        });
    }

    private Map<String, List<ScenarioTarget>> targets(ResourceManager manager) {
        Map<String, List<ScenarioTarget>> result = new LinkedHashMap<>();
        addTargets(manager, "echo_scene/scenes", ScenarioTarget.Kind.SCENE, result);
        addTargets(manager, "echo_story/stories", ScenarioTarget.Kind.STORY, result);
        return result;
    }

    private void addTargets(ResourceManager manager, String prefix, ScenarioTarget.Kind kind,
            Map<String, List<ScenarioTarget>> target) {
        manager.listResources(prefix, id -> id.getPath().endsWith(".es")).keySet().forEach(id -> {
            String resourceId = id.getNamespace() + ":" + stem(id);
            target.computeIfAbsent(resourceId, ignored -> new ArrayList<>()).add(new ScenarioTarget(kind, resourceId));
        });
    }

    private String stem(ResourceLocation id) {
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        return file.replaceFirst("(?i)(\\.[a-z-]+)?\\.es$", "");
    }

    private String title(String value) {
        return java.util.Arrays.stream(value.split("[_-]+"))
                .map(part -> part.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(java.util.stream.Collectors.joining());
    }

    private ScenarioId inferred(String path) {
        String value = path.substring("dev/scenarios/".length()).replaceFirst("(?i)\\.es$", "")
                .replace('/', '.').replace('-', '_');
        return new ScenarioId(value);
    }

    private record Snapshot(List<ScenarioDefinition> definitions, Map<ScenarioId, String> errors,
                            Map<String, ScenarioLocation> locations) { }
}
