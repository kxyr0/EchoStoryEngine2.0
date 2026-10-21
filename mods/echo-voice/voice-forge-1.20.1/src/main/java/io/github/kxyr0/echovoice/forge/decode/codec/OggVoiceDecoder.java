package io.github.kxyr0.echovoice.forge.decode.codec;

import com.mojang.blaze3d.audio.OggAudioStream;
import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceAudioDecoder;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.VoicePcmReadLimit;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OggVoiceDecoder implements VoiceAudioDecoder {
    @Override
    public VoiceAssetFormat format() {
        return VoiceAssetFormat.OGG;
    }

    @Override
    public DecodedPcm decode(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        verifySize(path, limits);
        try (InputStream input = Files.newInputStream(path); OggAudioStream ogg = new OggAudioStream(input)) {
            byte[] bytes = VoicePcmReadLimit.read(ogg.getFormat(), limits, () -> {
                ByteBuffer buffer = ogg.read(16_384);
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                return chunk;
            });
            DecodedPcm decoded = new DecodedPcm(bytes, Math.round(ogg.getFormat().getSampleRate()),
                    ogg.getFormat().getChannels());
            if (decoded.durationMillis() > limits.maximumDurationMillis()) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG,
                        "OGG exceeds configured duration.");
            }
            return decoded;
        } catch (VoiceDecodeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_DECODE_FAILED,
                    "OGG decode failed.", exception);
        }
    }

    private void verifySize(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        try {
            if (Files.size(path) > limits.maximumFileBytes()) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LARGE,
                        "OGG exceeds configured file size.");
            }
        } catch (IOException exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "OGG size could not be read.", exception);
        }
    }
}
