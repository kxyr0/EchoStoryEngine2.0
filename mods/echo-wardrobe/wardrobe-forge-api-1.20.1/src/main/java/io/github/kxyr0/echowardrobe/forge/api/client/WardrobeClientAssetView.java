package io.github.kxyr0.echowardrobe.forge.api.client;

import java.util.Optional;

/**
 * Client-side view over generated wardrobe assets.
 */
public interface WardrobeClientAssetView {
    Optional<String> worldSkinResource(String echoPlayerId, long appearanceRevision);

    Optional<String> faceSkinResource(String echoPlayerId, long appearanceRevision);
}
