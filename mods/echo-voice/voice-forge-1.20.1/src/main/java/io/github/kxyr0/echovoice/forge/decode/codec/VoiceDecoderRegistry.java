package io.github.kxyr0.echovoice.forge.decode.codec;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.forge.decode.VoiceAudioDecoder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class VoiceDecoderRegistry {
    private final Map<VoiceAssetFormat, VoiceAudioDecoder> decoders = new EnumMap<>(VoiceAssetFormat.class);

    public VoiceDecoderRegistry() {
        List.of(new WavVoiceDecoder(), new Mp3VoiceDecoder(), new OggVoiceDecoder())
                .forEach(decoder -> decoders.put(decoder.format(), decoder));
    }

    public VoiceAudioDecoder require(VoiceAssetFormat format) {
        VoiceAudioDecoder decoder = decoders.get(format);
        if (decoder == null) {
            throw new IllegalArgumentException("No decoder registered for " + format + ".");
        }
        return decoder;
    }
}
