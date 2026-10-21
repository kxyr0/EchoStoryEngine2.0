package io.github.kxyr0.echovoice.api.asset;

import java.util.Locale;
import java.util.Optional;

/** Supported authored file formats; extension is not part of asset identity. */
public enum VoiceAssetFormat {
    WAV,
    MP3,
    OGG;

    public String extension() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<VoiceAssetFormat> fromFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        for (VoiceAssetFormat format : values()) {
            if (lower.endsWith("." + format.extension())) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
