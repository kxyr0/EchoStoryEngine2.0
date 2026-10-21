package io.github.kxyr0.echonpcanimationadapter.resource;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpcanimationadapter.NpcAnimationBindingDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable last-known-good binding catalog.
 */
public record NpcAnimationBindingCatalog(
        List<NpcAnimationBindingDefinition> bindings,
        List<String> diagnostics
) {
    public NpcAnimationBindingCatalog {
        bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public Optional<NpcAnimationBindingDefinition> resolve(ActorId actorId, NpcDefinitionId definitionId) {
        Optional<NpcAnimationBindingDefinition> actor = bindings.stream()
                .filter(binding -> binding.actorId().filter(actorId::equals).isPresent())
                .findFirst();
        if (actor.isPresent()) {
            return actor;
        }
        return bindings.stream()
                .filter(binding -> binding.definitionId().filter(definitionId::equals).isPresent())
                .findFirst();
    }

    public boolean valid() {
        return diagnostics.isEmpty();
    }
}
