package io.github.kxyr0.echodialogue.core.model;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable rendered line from one linear dialogue node.
 */
public record CompiledDialogueLine(
        DialogueNodeId nodeId,
        int lineIndex,
        SpeakerId speakerId,
        String speakerDisplayName,
        Optional<SpeakerNameColor> speakerNameColor,
        Optional<SpeakerVisualRef> speakerVisual,
        String text,
        Optional<String> key,
        boolean thought,
        List<Cue> cues
) {
    public CompiledDialogueLine {
        nodeId = Objects.requireNonNull(nodeId, "nodeId");
        speakerId = Objects.requireNonNull(speakerId, "speakerId");
        speakerDisplayName = requireText(speakerDisplayName, "speakerDisplayName");
        speakerNameColor = Objects.requireNonNull(speakerNameColor, "speakerNameColor");
        speakerVisual = Objects.requireNonNull(speakerVisual, "speakerVisual");
        text = requireText(text, "text");
        key = Objects.requireNonNull(key, "key").map(value -> requireText(value, "key"));
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        if (lineIndex < 0) {
            throw new IllegalArgumentException("Line index must not be negative.");
        }
    }

    public CompiledDialogueLine(
            DialogueNodeId nodeId,
            int lineIndex,
            SpeakerId speakerId,
            String speakerDisplayName,
            String text
    ) {
        this(nodeId, lineIndex, speakerId, speakerDisplayName, Optional.empty(), Optional.empty(), text,
                Optional.empty(), false, List.of());
    }

    public CompiledDialogueLine(
            DialogueNodeId nodeId,
            int lineIndex,
            SpeakerId speakerId,
            String speakerDisplayName,
            Optional<SpeakerVisualRef> speakerVisual,
            String text,
            Optional<String> key,
            boolean thought,
            List<Cue> cues
    ) {
        this(nodeId, lineIndex, speakerId, speakerDisplayName, Optional.empty(), speakerVisual, text, key, thought,
                cues);
    }

    public CompiledDialogueLine(
            DialogueNodeId nodeId,
            int lineIndex,
            SpeakerId speakerId,
            String speakerDisplayName,
            Optional<SpeakerVisualRef> speakerVisual,
            String text
    ) {
        this(nodeId, lineIndex, speakerId, speakerDisplayName, Optional.empty(), speakerVisual, text,
                Optional.empty(), false, List.of());
    }

    public record Cue(String name, String value) {
        public Cue {
            name = requireText(name, "name");
            value = requireText(value, "value");
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
