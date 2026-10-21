package io.github.kxyr0.echovoice.forge.service;

import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOutcome;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackLifecycleListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VoicePlaybackListeners {
    private final List<VoicePlaybackLifecycleListener> listeners = new CopyOnWriteArrayList<>();

    public void add(VoicePlaybackLifecycleListener listener) {
        listeners.add(listener);
    }

    public void remove(VoicePlaybackLifecycleListener listener) {
        listeners.remove(listener);
    }

    public void publish(VoicePlaybackReceipt receipt) {
        for (VoicePlaybackLifecycleListener listener : listeners) {
            switch (receipt.outcome()) {
                case STARTED -> listener.started(receipt);
                case COMPLETED -> listener.completed(receipt);
                case INTERRUPTED -> listener.interrupted(receipt);
                case FAILED_ASSET, FAILED_AUDIO_DEVICE, FAILED_DECODE -> listener.failed(receipt);
                default -> {
                }
            }
        }
    }
}
