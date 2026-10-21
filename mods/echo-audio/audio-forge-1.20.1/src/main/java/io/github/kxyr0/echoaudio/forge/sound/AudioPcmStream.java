package io.github.kxyr0.echoaudio.forge.sound;

import io.github.kxyr0.echoaudio.pcm.AudioPcm;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

/** Whole-asset loop at the PCM stream boundary; no manual Play calls or iteration timer. */
public final class AudioPcmStream implements AudioStream {
    private final ByteBuffer cursor;
    private final AudioFormat format;
    private final boolean loop;
    private boolean closed;
    public AudioPcmStream(AudioPcm pcm, boolean loop) {
        cursor = pcm.cursor(); this.loop = loop;
        format = new AudioFormat(pcm.sampleRate(), 16, pcm.channels(), true, false);
    }
    @Override public AudioFormat getFormat() { return format; }
    @Override public ByteBuffer read(int requested) {
        int size = closed ? 0 : loop ? requested : Math.min(requested, cursor.remaining());
        ByteBuffer output = BufferUtils.createByteBuffer(size);
        while (output.hasRemaining()) {
            if (!cursor.hasRemaining()) cursor.rewind();
            int count = Math.min(output.remaining(), cursor.remaining());
            ByteBuffer part = cursor.duplicate(); part.limit(part.position() + count);
            output.put(part); cursor.position(cursor.position() + count);
        }
        return output.flip();
    }
    @Override public void close() { closed = true; }
}
