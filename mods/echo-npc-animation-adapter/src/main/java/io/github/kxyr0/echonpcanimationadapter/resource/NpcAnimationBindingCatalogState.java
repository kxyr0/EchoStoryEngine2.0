package io.github.kxyr0.echonpcanimationadapter.resource;

import java.util.List;

/**
 * Holds the active last-known-good adapter binding catalog.
 */
public final class NpcAnimationBindingCatalogState {
    private NpcAnimationBindingCatalog current = new NpcAnimationBindingCatalog(List.of(), List.of());
    private List<String> diagnostics = List.of();

    public NpcAnimationBindingCatalog current() {
        return current;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }

    public void install(NpcAnimationBindingCatalog candidate) {
        diagnostics = candidate.diagnostics();
        if (candidate.valid()) {
            current = candidate;
        }
    }
}
