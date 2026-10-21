package io.github.kxyr0.echoaudio.playback;

import io.github.kxyr0.echoaudio.api.*;
import java.util.*;

/** Playback identity differs from resource identity; semantic names belong to execution + audience. */
public final class AudioPlaybackRegistry<T> {
    public record Entry<T>(AudioPlaybackId id, String owner, String audience, AudioCue cue, T value) { }
    private final Map<AudioPlaybackId, Entry<T>> entries = new LinkedHashMap<>();
    public void add(Entry<T> entry) {
        if (entries.containsKey(entry.id())) throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", "Duplicate playback identity");
        if (entries.size() >= 1024) throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", "server capacity");
        if (!entry.cue().name().isEmpty() && !named(entry.owner(), entry.audience(), entry.cue().name()).isEmpty()) {
            throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", "Owned playback name already in use");
        }
        entries.put(entry.id(), entry);
    }
    public Entry<T> get(AudioPlaybackId id) { return entries.get(id); }
    public Entry<T> remove(AudioPlaybackId id) { return entries.remove(id); }
    public int size() { return entries.size(); }
    public Set<String> owners() { return entries.values().stream().map(Entry::owner).collect(java.util.stream.Collectors.toSet()); }
    public List<Entry<T>> named(String owner, String audience, String name) {
        return entries.values().stream().filter(e -> e.owner().equals(owner) && e.audience().equals(audience)
                && e.cue().name().equals(name)).toList();
    }
    public List<Entry<T>> finishOwner(String owner) {
        return removeMatching(e -> e.owner().equals(owner) && (e.cue().loop() || e.cue().waitForEnd()));
    }
    public List<Entry<T>> disconnect(String audience) { return removeMatching(e -> e.audience().equals(audience)); }
    private List<Entry<T>> removeMatching(java.util.function.Predicate<Entry<T>> predicate) {
        List<Entry<T>> removed = entries.values().stream().filter(predicate).toList();
        removed.forEach(e -> entries.remove(e.id())); return removed;
    }
}
