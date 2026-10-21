package io.github.kxyr0.echovoice.core.cache;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetRevision;
import java.util.Objects;

public record VoiceCacheKey(VoiceAssetKey assetKey, VoiceAssetRevision revision) {
    public VoiceCacheKey {
        assetKey = Objects.requireNonNull(assetKey, "assetKey");
        revision = Objects.requireNonNull(revision, "revision");
    }
}
