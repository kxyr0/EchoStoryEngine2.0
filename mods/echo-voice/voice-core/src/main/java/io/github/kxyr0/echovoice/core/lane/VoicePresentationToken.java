package io.github.kxyr0.echovoice.core.lane;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.Objects;

/** Complete authority token checked again after asynchronous decoding. */
public record VoicePresentationToken(
        DialogueSessionId executionId,
        int revision,
        long generation,
        VoiceAssetKey assetKey
) {
    public VoicePresentationToken {
        executionId = Objects.requireNonNull(executionId, "executionId");
        assetKey = Objects.requireNonNull(assetKey, "assetKey");
        if (revision < 0 || generation < 0) {
            throw new IllegalArgumentException("Presentation revision/generation must not be negative.");
        }
    }
}
