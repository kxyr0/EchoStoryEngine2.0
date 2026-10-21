package io.github.kxyr0.echonpc.api.form;

import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable read-only view of the current known-good form catalog.
 */
public record NpcFormCatalogView(long revision, List<NpcFormDefinition> forms, boolean truncated) {
    public NpcFormCatalogView {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must not be negative.");
        }
        forms = List.copyOf(Objects.requireNonNull(forms, "forms"));
    }

    public Optional<NpcFormDefinition> find(NpcFormId id) {
        Objects.requireNonNull(id, "id");
        return forms.stream().filter(form -> form.id().equals(id)).findFirst();
    }
}
