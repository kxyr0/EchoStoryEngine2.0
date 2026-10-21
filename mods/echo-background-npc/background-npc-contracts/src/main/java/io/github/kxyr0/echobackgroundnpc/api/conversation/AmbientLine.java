package io.github.kxyr0.echobackgroundnpc.api.conversation;

import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.Objects;
import java.util.Optional;

public record AmbientLine(String speaker, String text, Optional<String> voiceStem, BackgroundSourceRef source) {
    public AmbientLine {
        speaker = required(speaker, "speaker");
        text = required(text, "text");
        voiceStem = Objects.requireNonNull(voiceStem, "voiceStem").map(String::trim).filter(value -> !value.isEmpty());
        source = Objects.requireNonNull(source, "source");
    }
    private static String required(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank.");
        return checked;
    }
}
