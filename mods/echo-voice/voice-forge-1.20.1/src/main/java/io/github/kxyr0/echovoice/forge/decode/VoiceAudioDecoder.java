package io.github.kxyr0.echovoice.forge.decode;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import java.nio.file.Path;

public interface VoiceAudioDecoder {
    VoiceAssetFormat format();

    DecodedPcm decode(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException;
}
