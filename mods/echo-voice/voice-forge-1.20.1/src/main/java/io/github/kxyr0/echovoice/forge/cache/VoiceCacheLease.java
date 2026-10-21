package io.github.kxyr0.echovoice.forge.cache;

import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pins one decoded revision until playback/preload releases it. */
public final class VoiceCacheLease implements AutoCloseable {
    private final VoiceDecodedCache owner;
    private final VoiceCacheKey key;
    private final DecodedPcm pcm;
    private final AtomicBoolean closed = new AtomicBoolean();

    VoiceCacheLease(VoiceDecodedCache owner, VoiceCacheKey key, DecodedPcm pcm) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.key = Objects.requireNonNull(key, "key");
        this.pcm = Objects.requireNonNull(pcm, "pcm");
    }

    public DecodedPcm pcm() {
        return pcm;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            owner.release(key);
        }
    }
}
