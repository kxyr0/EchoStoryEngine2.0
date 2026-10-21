package io.github.kxyr0.echovoice.forge.decode.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VoiceDecoderTest {
    private static final VoiceDecodeLimits LIMITS = new VoiceDecodeLimits(10_000_000, 10_000);

    @TempDir
    Path temporary;

    @Test
    void wavDecodesToBoundedPcm() throws Exception {
        Path wav = temporary.resolve("line.wav");
        byte[] silence = new byte[44_100 * 2 / 10];
        AudioFormat format = new AudioFormat(44_100, 16, 1, true, false);
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(silence), format,
                silence.length / 2L)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, wav.toFile());
        }

        DecodedPcm decoded = new WavVoiceDecoder().decode(wav, LIMITS);

        assertEquals(100, decoded.durationMillis());
        assertEquals(1, decoded.audioFormat().getChannels());
    }

    @Test
    void pinnedJlayerMp3DecoderReadsRealFixture() throws Exception {
        DecodedPcm decoded = new Mp3VoiceDecoder().decode(resource("valid.mp3"), LIMITS);

        assertTrue(decoded.durationMillis() >= 2_000);
    }

    @Test
    void minecraftVorbisDecoderReadsRealFixture() throws Exception {
        DecodedPcm decoded = new OggVoiceDecoder().decode(resource("valid.ogg"), LIMITS);

        assertTrue(decoded.durationMillis() > 0);
    }

    @Test
    void vorbisRejectsLongOutputWhileReadingChunks() throws Exception {
        VoiceDecodeException error = assertThrows(VoiceDecodeException.class,
                () -> new OggVoiceDecoder().decode(resource("valid.ogg"), new VoiceDecodeLimits(10_000_000, 10)));
        assertEquals(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG, error.code());
    }

    @Test
    void corruptReplacementIsRejected() throws Exception {
        Path corrupt = temporary.resolve("broken.mp3");
        Files.writeString(corrupt, "not an mp3");

        VoiceDecodeException exception = assertThrows(VoiceDecodeException.class,
                () -> new Mp3VoiceDecoder().decode(corrupt, LIMITS));
        assertTrue(exception.code() == VoiceDiagnosticCode.VOICE_ASSET_DECODE_FAILED
                || exception.code() == VoiceDiagnosticCode.VOICE_ASSET_PCM_INVALID);
    }

    private Path resource(String name) throws Exception {
        return Path.of(VoiceDecoderTest.class.getResource("/audio/" + name).toURI());
    }
}
