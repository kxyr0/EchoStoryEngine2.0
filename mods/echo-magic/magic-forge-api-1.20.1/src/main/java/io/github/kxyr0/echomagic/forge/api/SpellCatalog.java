package io.github.kxyr0.echomagic.forge.api;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.view.SpellCatalogView;
import java.util.Optional;

public interface SpellCatalog {
    Optional<SpellDefinition> find(SpellId id);
    SpellCatalogView catalog();
}

