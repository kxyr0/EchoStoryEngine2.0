package io.github.kxyr0.echoaudio.forge;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.asset.AudioAssetDescriptor;
import io.github.kxyr0.echoaudio.forge.decode.VoiceDecoderBridge;
import io.github.kxyr0.echoaudio.forge.sound.AudioPcmStream;
import io.github.kxyr0.echoaudio.pcm.AudioPcm;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioDecoderTest {
    @org.junit.jupiter.api.io.TempDir Path temporary;
    @ParameterizedTest @ValueSource(strings = {"wav", "ogg", "mp3"})
    void existingCodecsDecodeRealGeneratedFixtures(String format) throws Exception {
        String filename = format.equals("wav") ? "test_tone.wav" : "test_loop." + format;
        Path file = Path.of(getClass().getResource("/audio/" + filename).toURI());
        AudioPcm pcm = new VoiceDecoderBridge().decode(new AudioAssetDescriptor(new AudioResourceId("test:tone"),
                new AudioRevision("test"), file, format));
        assertEquals(1, pcm.channels()); assertTrue(pcm.durationMillis() >= 450); assertTrue(pcm.durationMillis() < 1000);
    }
    @ParameterizedTest @ValueSource(strings = {"wav", "ogg", "mp3"})
    void corruptFilesFailCleanly(String format) throws Exception {
        Path file = java.nio.file.Files.writeString(temporary.resolve("bad." + format), "not audio");
        assertThrows(Exception.class, () -> new VoiceDecoderBridge().decode(new AudioAssetDescriptor(
                new AudioResourceId("test:bad"), new AudioRevision("test"), file, format)));
    }
    @Test void loopStreamWrapsSamplesAndOneShotReachesEofWithoutPlayRestarts() throws Exception {
        AudioPcm pcm = new AudioPcm(new byte[]{1, 0, 2, 0}, 8000, 1);
        try (var stream = new AudioPcmStream(pcm, true)) {
            var first = stream.read(12); assertEquals(12, first.remaining());
            assertEquals(1, first.get(0)); assertEquals(1, first.get(4)); assertEquals(2, first.get(10));
            assertEquals(12, stream.read(12).remaining());
        }
        try (var stream = new AudioPcmStream(pcm, false)) {
            assertEquals(4, stream.read(12).remaining()); assertEquals(0, stream.read(12).remaining());
        }
    }
}
