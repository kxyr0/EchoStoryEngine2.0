package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCatalog;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbol;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbolKind;
import io.github.kxyr0.echoscene.compile.SceneSymbol;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.SceneSymbolKind;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SourceSymbols {
    private SourceSymbols() { }

    static SceneSymbolCatalog scenes(List<Path> files) {
        List<SceneSymbol> values = new ArrayList<>();
        values.add(new SceneSymbol("John", "jdh:john", SceneSymbolKind.PLAYER));
        files.forEach(path -> sceneSymbol(path).ifPresent(values::add));
        return SceneSymbolCatalog.of(values);
    }

    static ScenarioCatalog scenarios(List<Path> files) {
        List<ScenarioSymbol> symbols = new ArrayList<>();
        Map<String, List<ScenarioTarget>> targets = new LinkedHashMap<>();
        files.forEach(path -> {
            String value = normalized(path);
            if (value.contains("/echo_npc/npcs/") && value.endsWith(".npc.es")) {
                symbols.add(scenarioSymbol(path, ScenarioSymbolKind.NPC));
            } else if (value.contains("/echo_observation/observations/") && value.endsWith(".es")) {
                symbols.add(scenarioSymbol(path, ScenarioSymbolKind.OBSERVATION));
            } else if (value.contains("/echo_scene/locations/") && value.endsWith(".json")) {
                symbols.add(scenarioSymbol(path, ScenarioSymbolKind.LOCATION));
            }
            if (value.contains("/echo_scene/scenes/") && value.endsWith(".es")) {
                addTarget(targets, resourceId(path), ScenarioTarget.Kind.SCENE);
            } else if (value.contains("/echo_story/stories/") && value.endsWith(".es")) {
                addTarget(targets, resourceId(path), ScenarioTarget.Kind.STORY);
            }
        });
        return new ScenarioCatalog() {
            public List<ScenarioSymbol> symbols() { return List.copyOf(symbols); }
            public List<ScenarioTarget> targets(String id) { return targets.getOrDefault(id, List.of()); }
        };
    }

    private static java.util.Optional<SceneSymbol> sceneSymbol(Path path) {
        String value = normalized(path);
        SceneSymbolKind kind = null;
        if (value.contains("/echo_npc/npcs/") && value.endsWith(".npc.es")) kind = SceneSymbolKind.NPC;
        else if (value.contains("/echo_dialogue/dialogues/") && value.endsWith(".dialogue.es")) {
            kind = SceneSymbolKind.DIALOGUE;
        } else if (value.contains("/echo_animation/animations/") && value.endsWith(".animation.es")) {
            kind = SceneSymbolKind.ANIMATION;
        } else if (value.contains("/echo_scene/scenes/") && value.endsWith(".es")) kind = SceneSymbolKind.SCENE;
        else if (value.contains("/echo_scene/locations/") && value.endsWith(".json")) kind = SceneSymbolKind.LOCATION;
        else if (value.contains("/echo_scene/cameras/") && value.endsWith(".json")) kind = SceneSymbolKind.CAMERA;
        return kind == null ? java.util.Optional.empty()
                : java.util.Optional.of(new SceneSymbol(title(stem(path)), resourceId(path), kind));
    }

    private static ScenarioSymbol scenarioSymbol(Path path, ScenarioSymbolKind kind) {
        return new ScenarioSymbol(title(stem(path)), resourceId(path), kind);
    }

    private static void addTarget(Map<String, List<ScenarioTarget>> targets, String id, ScenarioTarget.Kind kind) {
        targets.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new ScenarioTarget(kind, id));
    }

    private static String resourceId(Path path) {
        String value = normalized(path);
        String[] parts = value.split("/");
        String namespace = "unknown";
        for (int index = 0; index + 1 < parts.length; index++) {
            if (parts[index].equals("data")) namespace = parts[index + 1];
        }
        return namespace + ":" + stem(path);
    }

    private static String stem(Path path) {
        return path.getFileName().toString().replaceFirst("(?i)(\\.[a-z-]+)?\\.(es|json)$", "");
    }

    private static String normalized(Path path) { return path.toString().replace('\\', '/'); }
    private static String title(String value) {
        return java.util.Arrays.stream(value.split("[_-]+"))
                .map(part -> part.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(java.util.stream.Collectors.joining());
    }
}
