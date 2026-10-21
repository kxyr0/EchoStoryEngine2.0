package io.github.kxyr0.echoaudio.forge.server;

import java.util.*;
import net.minecraftforge.eventbus.api.Event;

/** Resolves through an installed location provider. This event owns no location registry. */
public final class AudioLocationLookupEvent extends Event {
    public record Position(String dimension, double x, double y, double z) { }
    private final String reference;
    private final List<Position> matches = new ArrayList<>();
    public AudioLocationLookupEvent(String reference) { this.reference = reference; }
    public String reference() { return reference; }
    public void provide(Position position) { matches.add(position); }
    public Position require() {
        if (matches.size() != 1) throw new io.github.kxyr0.echoaudio.api.AudioFailure(
                "AUDIO_LOCATION_NOT_FOUND", reference + " resolved to " + matches.size() + " locations");
        return matches.get(0);
    }
}
