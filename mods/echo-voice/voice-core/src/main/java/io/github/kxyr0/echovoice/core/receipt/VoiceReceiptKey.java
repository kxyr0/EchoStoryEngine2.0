package io.github.kxyr0.echovoice.core.receipt;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.Objects;

public record VoiceReceiptKey(DialogueSessionId executionId, int presentationRevision, VoiceAssetKey assetKey) {
    public VoiceReceiptKey {
        executionId = Objects.requireNonNull(executionId, "executionId");
        assetKey = Objects.requireNonNull(assetKey, "assetKey");
        if (presentationRevision < 0) {
            throw new IllegalArgumentException("Presentation revision must not be negative.");
        }
    }
}
