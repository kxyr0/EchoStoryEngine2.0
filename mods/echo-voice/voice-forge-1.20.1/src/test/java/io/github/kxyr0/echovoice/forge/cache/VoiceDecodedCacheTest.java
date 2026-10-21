package io.github.kxyr0.echovoice.forge.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetRevision;
import io.github.kxyr0.echovoice.core.cache.VoiceCacheKey;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import org.junit.jupiter.api.Test;

class VoiceDecodedCacheTest {
    @Test
    void activeRevisionLeasePinsBytesUntilRelease() throws Exception {
        VoiceDecodedCache cache = new VoiceDecodedCache(8);
        VoiceCacheKey first = key("a", 1);
        VoiceCacheKey second = key("b", 1);
        cache.put(first, pcm());
        VoiceCacheLease lease = cache.acquire(first).orElseThrow();
        cache.put(second, pcm());

        assertTrue(cache.acquire(first).isPresent());
        lease.close();
        assertTrue(cache.bytes() <= 8);
    }

    @Test
    void hitMissMetricsAreDeterministic() throws Exception {
        VoiceDecodedCache cache = new VoiceDecodedCache(64);
        VoiceCacheKey present = key("a", 1);
        cache.put(present, pcm());
        cache.acquire(present).orElseThrow().close();
        cache.acquire(key("missing", 1));

        assertEquals(0.5, cache.hitRatio());
    }

    private DecodedPcm pcm() throws Exception {
        return new DecodedPcm(new byte[8], 8_000, 1);
    }

    private VoiceCacheKey key(String stem, long revision) {
        return new VoiceCacheKey(VoiceAssetKey.of(DialogueId.parse("jdh:first_morning"), stem),
                new VoiceAssetRevision(revision));
    }
}
