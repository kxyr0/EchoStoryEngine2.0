package io.github.kxyr0.echomagic.core.catalog;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.core.validation.SpellDefinitionValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class SpellCatalog {
    private final AtomicReference<SpellCatalogSnapshot> current = new AtomicReference<>(SpellCatalogSnapshot.empty());
    private final SpellDefinitionValidator validator;

    public SpellCatalog(SpellDefinitionValidator validator) { this.validator = validator; }

    public SpellCatalogPublishResult publish(List<SpellDefinition> definitions) {
        List<MagicDiagnostic> diagnostics = new ArrayList<>();
        LinkedHashMap<SpellId, SpellDefinition> candidate = new LinkedHashMap<>();
        for (SpellDefinition definition : definitions) {
            diagnostics.addAll(validator.validate(definition, definition.id().value()));
            if (candidate.putIfAbsent(definition.id(), definition) != null) {
                diagnostics.add(MagicDiagnostic.error(MagicDiagnosticCode.MAGIC_SPELL_INVALID,
                        "Duplicate spell id.", definition.id().value()));
            }
        }
        if (diagnostics.stream().anyMatch(MagicDiagnostic::isError)) {
            return new SpellCatalogPublishResult(false, current.get().revision(), diagnostics);
        }
        long revision = current.get().revision() + 1L;
        current.set(new SpellCatalogSnapshot(candidate, revision));
        return new SpellCatalogPublishResult(true, revision, diagnostics);
    }

    public Optional<SpellDefinition> find(SpellId id) { return current.get().find(id); }
    public SpellCatalogSnapshot snapshot() { return current.get(); }
}

