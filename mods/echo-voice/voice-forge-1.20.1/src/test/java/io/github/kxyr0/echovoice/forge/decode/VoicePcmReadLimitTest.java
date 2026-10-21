package io.github.kxyr0.echovoice.forge.decode;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoicePcmReadLimitTest {
    @Test
    void overlongDecodedStreamStopsBeforeReadingTheWholeInput() {
        AtomicInteger chunks = new AtomicInteger();
        var error = assertThrows(VoiceDecodeException.class, () -> VoicePcmReadLimit.read(
                new AudioFormat(8000, 16, 1, true, false), new VoiceDecodeLimits(100000, 100), () -> {
                    chunks.incrementAndGet();
                    return new byte[800];
                }));
        assertEquals(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG, error.code());
        assertEquals(3, chunks.get());
    }

    @Test
    void exactDurationAndNormalEofAreAccepted() throws Exception {
        AtomicInteger chunks = new AtomicInteger();
        byte[] pcm = VoicePcmReadLimit.read(new AudioFormat(8000, 16, 1, true, false),
                new VoiceDecodeLimits(100000, 100), () -> chunks.getAndIncrement() < 2 ? new byte[800] : new byte[0]);
        assertEquals(1600, pcm.length);
    }
}
