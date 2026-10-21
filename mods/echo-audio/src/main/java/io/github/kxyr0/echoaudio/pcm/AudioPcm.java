package io.github.kxyr0.echoaudio.pcm;

import java.nio.ByteBuffer;

/** Immutable owned signed little-endian 16-bit PCM. Read cursors do not copy the cached samples. */
public final class AudioPcm {
    private final byte[] samples;
    private final int sampleRate;
    private final int channels;
    public AudioPcm(byte[] samples, int sampleRate, int channels) {
        if (sampleRate < 8000 || sampleRate > 192000 || channels < 1 || channels > 2
                || samples.length == 0 || samples.length % (channels * 2) != 0) {
            throw new IllegalArgumentException("Invalid PCM");
        }
        this.samples = samples.clone(); this.sampleRate = sampleRate; this.channels = channels;
    }
    public int size() { return samples.length; }
    public int sampleRate() { return sampleRate; }
    public int channels() { return channels; }
    public long durationMillis() { return (long) size() * 1000 / (sampleRate * channels * 2); }
    public ByteBuffer cursor() { return ByteBuffer.wrap(samples).asReadOnlyBuffer(); }
    public AudioPcm mono() {
        if (channels == 1) return this;
        byte[] mono = new byte[samples.length / 2];
        for (int i = 0, j = 0; i < samples.length; i += 4, j += 2) {
            int left = (short) ((samples[i] & 255) | samples[i + 1] << 8);
            int right = (short) ((samples[i + 2] & 255) | samples[i + 3] << 8);
            short mixed = (short) ((left + right) / 2);
            mono[j] = (byte) mixed; mono[j + 1] = (byte) (mixed >> 8);
        }
        return new AudioPcm(mono, sampleRate, 1);
    }
}
