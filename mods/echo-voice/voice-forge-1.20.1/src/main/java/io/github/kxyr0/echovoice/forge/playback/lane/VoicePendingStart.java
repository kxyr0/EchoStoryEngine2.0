package io.github.kxyr0.echovoice.forge.playback.lane;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.forge.cache.VoiceCacheLease;

final class VoicePendingStart {
    final VoicePlaybackId id;
    final VoicePlaybackRequest request;
    final VoiceAssetSnapshot asset;
    final long eligibleTick;
    VoiceCacheLease lease;
    long decodeLatencyMillis;
    boolean cacheHit;

    VoicePendingStart(VoicePlaybackId id, VoicePlaybackRequest request, VoiceAssetSnapshot asset, long eligibleTick) {
        this.id = id;
        this.request = request;
        this.asset = asset;
        this.eligibleTick = eligibleTick;
    }

    void ready(VoiceCacheLease nextLease, long latency, boolean hit) {
        lease = nextLease;
        decodeLatencyMillis = latency;
        cacheHit = hit;
    }

    void closeLease() {
        if (lease != null) {
            lease.close();
            lease = null;
        }
    }
}
