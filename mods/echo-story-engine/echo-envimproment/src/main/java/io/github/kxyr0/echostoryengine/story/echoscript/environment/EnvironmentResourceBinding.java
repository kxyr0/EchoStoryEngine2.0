package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record EnvironmentResourceBinding(
        Optional<EnvironmentResourceDef> resource,
        List<StoryDiagnostic> diagnostics) {
    EnvironmentResourceBinding {
        resource = Objects.requireNonNull(resource, "resource");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    boolean succeeded() {
        return resource.isPresent() && diagnostics.isEmpty();
    }
}
