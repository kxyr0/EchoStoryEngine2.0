package io.github.kxyr0.echovoice.forge.api;

/** Aggregate read-only view used by diagnostics and adapters. */
public interface VoiceQueryService {
    VoiceAssetCatalog assets();

    VoicePlaybackView playback();

    VoiceDiagnosticsView diagnostics();
}
