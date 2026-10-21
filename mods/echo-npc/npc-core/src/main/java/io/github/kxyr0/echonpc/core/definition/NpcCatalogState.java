package io.github.kxyr0.echonpc.core.definition;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic known-good catalog reference. Failed candidate reloads never mutate this state.
 */
public final class NpcCatalogState {
    private final AtomicReference<NpcDefinitionCatalog> current = new AtomicReference<>(NpcDefinitionCatalog.empty());

    public NpcDefinitionCatalog current() {
        return current.get();
    }

    public NpcDefinitionCatalog install(NpcDefinitionCatalog catalog) {
        current.set(Objects.requireNonNull(catalog, "catalog"));
        return catalog;
    }
}
