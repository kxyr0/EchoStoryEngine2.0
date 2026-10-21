package io.github.kxyr0.echovoice.forge.decode;

import java.util.Arrays;
import javax.sound.sampled.AudioFormat;

/** Internal normalized little-endian signed 16-bit PCM. */
public final class DecodedPcm {
    private final byte[] bytes;
    private final int sampleRate;
    private final int channels;

    public DecodedPcm(byte[] bytes, int sampleRate, int channels) throws VoiceDecodeException {
        if (sampleRate < 8_000 || sampleRate > 192_000 || (channels != 1 && channels != 2)) {
            throw new VoiceDecodeException(
                    io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode.VOICE_ASSET_PCM_INVALID,
                    "PCM sample rate/channels are unsupported.");
        }
        int frameSize = channels * Short.BYTES;
        if (bytes.length == 0 || bytes.length % frameSize != 0) {
            throw new VoiceDecodeException(
                    io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode.VOICE_ASSET_PCM_INVALID,
                    "PCM contains zero or malformed frames.");
        }
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public int byteSize() {
        return bytes.length;
    }

    public long durationMillis() {
        return Math.multiplyExact((long) bytes.length, 1000L) / (sampleRate * channels * Short.BYTES);
    }

    public AudioFormat audioFormat() {
        return new AudioFormat(sampleRate, 16, channels, true, false);
    }
}
