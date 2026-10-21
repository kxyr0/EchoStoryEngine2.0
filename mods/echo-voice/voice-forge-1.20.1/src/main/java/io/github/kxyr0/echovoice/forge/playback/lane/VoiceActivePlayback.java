package io.github.kxyr0.echovoice.forge.playback.lane;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.cache.VoiceCacheLease;
import io.github.kxyr0.echovoice.forge.playback.sound.VoiceSoundInstance;

final class VoiceActivePlayback {
    final VoicePlaybackId id;
    final VoicePlaybackRequest request;
    final VoiceAssetSnapshot asset;
    final VoiceCacheLease lease;
    final VoiceSoundInstance sound;
    final long startedAtMillis;
    final long engineGeneration;
    VoiceStopReason stopReason;

    VoiceActivePlayback(VoicePendingStart pending, VoiceSoundInstance sound, long engineGeneration) {
        id = pending.id;
        request = pending.request;
        asset = pending.asset;
        lease = pending.lease;
        this.sound = sound;
        startedAtMillis = System.currentTimeMillis();
        this.engineGeneration = engineGeneration;
    }

    long playedMillis() {
        return Math.max(0, Math.min(asset.durationMillis(), System.currentTimeMillis() - startedAtMillis));
    }
}
