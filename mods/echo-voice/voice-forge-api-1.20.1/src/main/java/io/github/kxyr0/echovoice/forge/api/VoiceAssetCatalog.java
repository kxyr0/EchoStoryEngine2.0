package io.github.kxyr0.echovoice.forge.api;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.List;
import java.util.Optional;

public interface VoiceAssetCatalog {
    Optional<VoiceAssetSnapshot> find(VoiceAssetKey key);

    List<VoiceAssetSnapshot> assets();
}
