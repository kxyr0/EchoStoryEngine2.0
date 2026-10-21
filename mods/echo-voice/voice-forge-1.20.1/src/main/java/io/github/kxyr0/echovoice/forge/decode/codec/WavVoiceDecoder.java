package io.github.kxyr0.echovoice.forge.decode.codec;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceAudioDecoder;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import io.github.kxyr0.echovoice.forge.decode.VoicePcmReadLimit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class WavVoiceDecoder implements VoiceAudioDecoder {
    @Override
    public VoiceAssetFormat format() {
        return VoiceAssetFormat.WAV;
    }

    @Override
    public DecodedPcm decode(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        verifySize(path, limits);
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
            int channels = source.getFormat().getChannels();
            float rate = source.getFormat().getSampleRate();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels,
                    channels * Short.BYTES, rate, false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, source)) {
                byte[] bytes = VoicePcmReadLimit.read(target, limits, () -> pcm.readNBytes(16_384));
                return validated(bytes, Math.round(rate), channels, limits);
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_FORMAT_UNSUPPORTED,
                    "WAV codec is unsupported.", exception);
        } catch (IOException exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "WAV could not be read.", exception);
        }
    }

    private void verifySize(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        try {
            if (Files.size(path) > limits.maximumFileBytes()) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LARGE,
                        "WAV exceeds configured file size.");
            }
        } catch (IOException exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "WAV size could not be read.", exception);
        }
    }

    private DecodedPcm validated(byte[] bytes, int rate, int channels, VoiceDecodeLimits limits)
            throws VoiceDecodeException {
        DecodedPcm decoded = new DecodedPcm(bytes, rate, channels);
        if (decoded.durationMillis() > limits.maximumDurationMillis()) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG,
                    "WAV exceeds configured duration.");
        }
        return decoded;
    }
}
