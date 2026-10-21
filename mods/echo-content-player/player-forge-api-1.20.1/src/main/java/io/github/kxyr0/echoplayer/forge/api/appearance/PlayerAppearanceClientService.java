package io.github.kxyr0.echoplayer.forge.api.appearance;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import java.util.Optional;

/**
 * Client-side immutable appearance mirror.
 */
public interface PlayerAppearanceClientService {
    Optional<PlayerAppearanceSnapshot> appearance(EchoPlayerId playerId);
}
