package io.github.kxyr0.echovoice.forge.api;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;

/** Public command surface for the one local foreground voice lane. */
public interface VoiceService {
    VoicePlaybackReceipt play(VoicePlaybackRequest request);

    VoicePlaybackReceipt stop(VoiceStopReason reason);

    void preload(VoiceAssetKey assetKey);

    void addLifecycleListener(VoicePlaybackLifecycleListener listener);

    void removeLifecycleListener(VoicePlaybackLifecycleListener listener);
}
