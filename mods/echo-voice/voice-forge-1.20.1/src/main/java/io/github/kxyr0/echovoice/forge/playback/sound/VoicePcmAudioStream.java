package io.github.kxyr0.echovoice.forge.playback.sound;

import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

/** One-shot Minecraft audio stream over a pinned immutable PCM revision. */
public final class VoicePcmAudioStream implements AudioStream {
    private final byte[] bytes;
    private final AudioFormat format;
    private int position;

    public VoicePcmAudioStream(DecodedPcm pcm) {
        bytes = pcm.copyBytes();
        format = pcm.audioFormat();
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int requestedBytes) {
        if (position >= bytes.length) {
            return null;
        }
        int count = Math.min(requestedBytes, bytes.length - position);
        ByteBuffer output = BufferUtils.createByteBuffer(count);
        output.put(bytes, position, count).flip();
        position += count;
        return output;
    }

    @Override
    public void close() {
        position = bytes.length;
    }
}
