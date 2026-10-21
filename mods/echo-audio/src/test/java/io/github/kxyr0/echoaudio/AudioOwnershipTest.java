package io.github.kxyr0.echoaudio;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.playback.AudioPlaybackRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioOwnershipTest {
    private AudioPlaybackRegistry.Entry<String> entry(String owner, String audience, String operation, String name) {
        return new AudioPlaybackRegistry.Entry<>(AudioPlaybackId.next(), owner, audience,
                new AudioCue(operation, "jdh:rain", name, "", new AudioOptions(1, 1, 0, 0, 0)), "backend");
    }
    @Test void cleanupStopsOnlyOwnedLoopsAndBlockingAudioAndKeepsOneShot() {
        var registry = new AudioPlaybackRegistry<String>();
        var loop = entry("scene:a", "alice", "Loop", "Rain");
        var foreign = entry("system:weather", "alice", "Loop", "Rain");
        var shot = entry("scene:a", "alice", "Play", "");
        var waiting = entry("scene:a", "alice", "PlayAndWait", "");
        registry.add(loop); registry.add(foreign); registry.add(shot); registry.add(waiting);
        var removed = registry.finishOwner("scene:a");
        assertEquals(2, removed.size()); assertTrue(removed.contains(loop)); assertTrue(removed.contains(waiting));
        assertNotNull(registry.get(shot.id())); assertNotNull(registry.get(foreign.id()));
        assertEquals(2, registry.size()); assertTrue(registry.finishOwner("scene:a").isEmpty());
    }
    @Test void sameResourceHasIndependentIdentitiesAndNamesAreScopedToAudienceAndOwner() {
        var registry = new AudioPlaybackRegistry<String>();
        var a = entry("scene:a", "alice", "Loop", "Rain"); var b = entry("scene:b", "alice", "Loop", "Rain");
        var c = entry("scene:a", "bob", "Loop", "Rain"); registry.add(a); registry.add(b); registry.add(c);
        assertEquals(a.id(), registry.named("scene:a", "alice", "Rain").get(0).id());
        assertThrows(AudioFailure.class, () -> registry.add(entry("scene:a", "alice", "Loop", "Rain")));
        assertEquals(2, registry.disconnect("alice").size()); assertNotNull(registry.get(c.id()));
    }
}
