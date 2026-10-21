package io.github.kxyr0.echoitem.api.definition;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Definition plus bounded diagnostics produced by compile, validate, or reload operations.
 */
public record ItemDefinitionResult<T extends ItemDefinition>(
        Optional<T> definition,
        List<ItemDefinitionDiagnostic> diagnostics
) {
    public ItemDefinitionResult {
        definition = Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (definition.isPresent() && diagnostics.stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            throw new IllegalArgumentException("Invalid definitions must not be published.");
        }
    }

    public static <T extends ItemDefinition> ItemDefinitionResult<T> valid(
            T definition,
            List<ItemDefinitionDiagnostic> diagnostics
    ) {
        return new ItemDefinitionResult<>(Optional.of(definition), diagnostics);
    }

    public static <T extends ItemDefinition> ItemDefinitionResult<T> invalid(
            List<ItemDefinitionDiagnostic> diagnostics
    ) {
        return new ItemDefinitionResult<>(Optional.empty(), diagnostics);
    }

    public boolean successful() {
        return definition.isPresent() && diagnostics.stream().noneMatch(ItemDefinitionDiagnostic::error);
    }
}
