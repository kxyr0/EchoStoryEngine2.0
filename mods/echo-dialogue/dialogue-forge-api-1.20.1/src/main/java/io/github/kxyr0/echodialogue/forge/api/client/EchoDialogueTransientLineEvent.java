package io.github.kxyr0.echodialogue.forge.api.client;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Requests a client-only, non-interactive line rendered by the standard Dialogue overlay. */
public final class EchoDialogueTransientLineEvent extends Event {
    private final String presentationId;
    private final String speakerDisplayName;
    private final String text;
    private final long durationMillis;

    public EchoDialogueTransientLineEvent(String presentationId, String speakerDisplayName,
            String text, long durationMillis) {
        this.presentationId = requireText(presentationId, "presentationId");
        this.speakerDisplayName = requireText(speakerDisplayName, "speakerDisplayName");
        this.text = requireText(text, "text");
        if (durationMillis < 1L || durationMillis > 60_000L) {
            throw new IllegalArgumentException("durationMillis must be within 1..60000.");
        }
        this.durationMillis = durationMillis;
    }

    public String presentationId() { return presentationId; }
    public String speakerDisplayName() { return speakerDisplayName; }
    public String text() { return text; }
    public long durationMillis() { return durationMillis; }

    private static String requireText(String value, String name) {
        String result = Objects.requireNonNull(value, name).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return result;
    }
}
