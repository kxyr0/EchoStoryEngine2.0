package io.github.kxyr0.echovoice.forge.service;

import io.github.kxyr0.echovoice.forge.api.VoiceAssetCatalog;
import io.github.kxyr0.echovoice.forge.api.VoiceDiagnosticsView;
import io.github.kxyr0.echovoice.forge.api.VoicePlaybackView;
import io.github.kxyr0.echovoice.forge.api.VoiceQueryService;
import java.util.Objects;

public record ForgeVoiceQueryService(
        VoiceAssetCatalog assets,
        VoicePlaybackView playback,
        VoiceDiagnosticsView diagnostics
) implements VoiceQueryService {
    public ForgeVoiceQueryService {
        assets = Objects.requireNonNull(assets, "assets");
        playback = Objects.requireNonNull(playback, "playback");
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }
}
