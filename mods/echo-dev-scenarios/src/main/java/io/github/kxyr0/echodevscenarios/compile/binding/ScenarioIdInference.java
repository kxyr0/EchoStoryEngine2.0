package io.github.kxyr0.echodevscenarios.compile.binding;

import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ScenarioIdInference {
    ScenarioId id(String sourcePath) {
        String path = sourcePath.replace('\\', '/').replaceFirst("(?i)\\.es$", "");
        int marker = path.indexOf("dev/scenarios/");
        if (marker >= 0) path = path.substring(marker + "dev/scenarios/".length());
        String normalized = path.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_/]+", "_").toLowerCase(Locale.ROOT).replace('/', '.');
        return new ScenarioId(normalized);
    }

    List<String> groups(ScenarioId id) {
        String[] parts = id.value().split("\\.");
        return parts.length <= 1 ? List.of() : Arrays.asList(parts).subList(0, parts.length - 1);
    }

    String title(String className) {
        return className.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    }
}
