package io.github.kxyr0.echovoice.forge.catalog;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetSnapshot;
import java.nio.file.Path;
import java.util.Objects;

public record VoiceAssetEntry(VoiceAssetSnapshot snapshot, Path path) {
    public VoiceAssetEntry {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
