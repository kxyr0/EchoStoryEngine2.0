package io.github.kxyr0.echovoice.api.identity;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import java.util.Objects;

/** Extension-independent runtime identity: DialogueId plus filename stem. */
public record VoiceAssetKey(DialogueId dialogueId, VoiceFileStem stem) {
    public VoiceAssetKey {
        dialogueId = Objects.requireNonNull(dialogueId, "dialogueId");
        stem = Objects.requireNonNull(stem, "stem");
    }

    public static VoiceAssetKey of(DialogueId dialogueId, String stem) {
        return new VoiceAssetKey(dialogueId, VoiceFileStem.parse(stem));
    }

    @Override
    public String toString() {
        return dialogueId + "/" + stem;
    }
}
