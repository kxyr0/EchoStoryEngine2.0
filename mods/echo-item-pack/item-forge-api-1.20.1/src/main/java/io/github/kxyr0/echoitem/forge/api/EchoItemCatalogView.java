package io.github.kxyr0.echoitem.forge.api;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Public read-only item catalog view exposed by the Forge host.
 */
public interface EchoItemCatalogView {
    Optional<WeaponDefinition> weapon(EchoItemId id);

    List<EchoItemId> weaponIds();

    long revision();
}
