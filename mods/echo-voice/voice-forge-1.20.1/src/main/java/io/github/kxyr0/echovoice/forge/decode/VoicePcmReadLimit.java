package io.github.kxyr0.echovoice.forge.decode;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;

/** Enforces duration before accumulating unbounded decoded PCM, including highly compressed input. */
public final class VoicePcmReadLimit {
    private VoicePcmReadLimit() { }

    public static byte[] read(AudioFormat format, VoiceDecodeLimits limits, ChunkReader reader)
            throws IOException, VoiceDecodeException {
        int channels = format.getChannels();
        int rate = Math.round(format.getSampleRate());
        if (channels < 1 || channels > 2 || rate < 8_000 || rate > 192_000) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_PCM_INVALID,
                    "PCM sample rate/channels are unsupported.");
        }
        long duration = Math.min(limits.maximumDurationMillis(), Integer.MAX_VALUE);
        long maximum = Math.min(Integer.MAX_VALUE - 8L, duration * rate * channels * Short.BYTES / 1000L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk;
        while ((chunk = reader.read()).length > 0) {
            if ((long) output.size() + chunk.length > maximum) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG,
                        "Decoded PCM exceeds configured duration/size limit.");
            }
            output.writeBytes(chunk);
        }
        return output.toByteArray();
    }

    @FunctionalInterface
    public interface ChunkReader {
        byte[] read() throws IOException;
    }
}
