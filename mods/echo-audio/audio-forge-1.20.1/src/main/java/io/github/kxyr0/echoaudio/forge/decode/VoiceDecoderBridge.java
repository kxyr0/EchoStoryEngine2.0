package io.github.kxyr0.echoaudio.forge.decode;

import io.github.kxyr0.echoaudio.asset.AudioAssetDescriptor;
import io.github.kxyr0.echoaudio.pcm.AudioDecoder;
import io.github.kxyr0.echoaudio.pcm.AudioPcm;
import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.codec.VoiceDecoderRegistry;
import java.util.Locale;

/** Reuses the existing stateless codecs only. No Voice lanes, receipts, channels or transcript dependency. */
public final class VoiceDecoderBridge implements AudioDecoder {
    private final VoiceDecoderRegistry codecs = new VoiceDecoderRegistry();
    @Override public AudioPcm decode(AudioAssetDescriptor asset) throws Exception {
        var format = VoiceAssetFormat.valueOf(asset.format().toUpperCase(Locale.ROOT));
        var pcm = codecs.require(format).decode(asset.path(), new VoiceDecodeLimits(16 * 1024 * 1024, 120_000));
        return new AudioPcm(pcm.copyBytes(), Math.round(pcm.audioFormat().getSampleRate()), pcm.audioFormat().getChannels());
    }
}
