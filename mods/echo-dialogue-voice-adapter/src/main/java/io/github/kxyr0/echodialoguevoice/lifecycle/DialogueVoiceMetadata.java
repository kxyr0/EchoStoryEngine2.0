package io.github.kxyr0.echodialoguevoice.lifecycle;

import io.github.kxyr0.echodialogue.forge.api.client.DialogueLineCue;
import io.github.kxyr0.echovoice.api.identity.VoiceFileStem;
import java.util.List;
import java.util.Optional;

/** Parsed immutable voice cues from one committed line snapshot. */
record DialogueVoiceMetadata(VoiceFileStem stem, double volume) {
    static Optional<DialogueVoiceMetadata> from(List<DialogueLineCue> cues) {
        Optional<String> stem = value(cues, "voice.stem");
        if (stem.isEmpty()) {
            return Optional.empty();
        }
        try {
            double volume = value(cues, "voice.volume").map(Double::parseDouble).orElse(1.0);
            if (!Double.isFinite(volume) || volume < 0.0 || volume > 1.0) {
                return Optional.empty();
            }
            return Optional.of(new DialogueVoiceMetadata(VoiceFileStem.parse(stem.orElseThrow()), volume));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> value(List<DialogueLineCue> cues, String name) {
        List<String> values = cues.stream().filter(cue -> cue.name().equals(name))
                .map(DialogueLineCue::value).toList();
        return values.size() == 1 ? Optional.of(values.get(0)) : Optional.empty();
    }
}
