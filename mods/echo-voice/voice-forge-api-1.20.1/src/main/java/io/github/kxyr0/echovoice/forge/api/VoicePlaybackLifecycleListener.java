package io.github.kxyr0.echovoice.forge.api;

import io.github.kxyr0.echovoice.api.playback.VoicePlaybackReceipt;

/** Future animation/lip-sync adapters observe outcomes without audio internals. */
public interface VoicePlaybackLifecycleListener {
    default void started(VoicePlaybackReceipt receipt) {
    }

    default void completed(VoicePlaybackReceipt receipt) {
    }

    default void interrupted(VoicePlaybackReceipt receipt) {
    }

    default void failed(VoicePlaybackReceipt receipt) {
    }
}
