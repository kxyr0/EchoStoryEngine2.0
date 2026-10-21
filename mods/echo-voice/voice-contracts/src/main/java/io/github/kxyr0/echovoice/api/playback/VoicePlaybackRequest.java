package io.github.kxyr0.echovoice.api.playback;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.Objects;

/** Immutable request for the single local foreground voice lane. */
public record VoicePlaybackRequest(
        DialogueSessionId dialogueExecutionId,
        int linePresentationRevision,
        long presentationGeneration,
        VoiceAssetKey assetKey,
        double volume,
        VoicePlaybackOwner owner
) {
    public VoicePlaybackRequest {
        dialogueExecutionId = Objects.requireNonNull(dialogueExecutionId, "dialogueExecutionId");
        assetKey = Objects.requireNonNull(assetKey, "assetKey");
        owner = Objects.requireNonNull(owner, "owner");
        if (linePresentationRevision < 0 || presentationGeneration < 0) {
            throw new IllegalArgumentException("Presentation revision/generation must not be negative.");
        }
        if (!Double.isFinite(volume) || volume < 0.0 || volume > 1.0) {
            throw new IllegalArgumentException("Voice volume must be in range 0.0 .. 1.0.");
        }
    }
}
