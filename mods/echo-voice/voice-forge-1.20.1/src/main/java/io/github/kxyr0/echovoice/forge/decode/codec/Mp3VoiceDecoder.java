package io.github.kxyr0.echovoice.forge.decode.codec;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import io.github.kxyr0.echovoice.forge.decode.VoiceAudioDecoder;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeException;
import io.github.kxyr0.echovoice.forge.decode.VoiceDecodeLimits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public final class Mp3VoiceDecoder implements VoiceAudioDecoder {
    @Override
    public VoiceAssetFormat format() {
        return VoiceAssetFormat.MP3;
    }

    @Override
    public DecodedPcm decode(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        verifySize(path, limits);
        try (InputStream input = Files.newInputStream(path)) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int rate = 0;
            int channels = 0;
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (rate == 0) {
                    rate = samples.getSampleFrequency();
                    channels = samples.getChannelCount();
                }
                writeLittleEndian(samples, output);
                bitstream.closeFrame();
                rejectOversizePcm(output.size(), rate, channels, limits);
            }
            DecodedPcm decoded = new DecodedPcm(output.toByteArray(), rate, channels);
            if (decoded.durationMillis() > limits.maximumDurationMillis()) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG,
                        "MP3 exceeds configured duration.");
            }
            return decoded;
        } catch (VoiceDecodeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_DECODE_FAILED,
                    "MP3 decode failed.", exception);
        }
    }

    private void writeLittleEndian(SampleBuffer samples, ByteArrayOutputStream output) {
        short[] buffer = samples.getBuffer();
        for (int index = 0; index < samples.getBufferLength(); index++) {
            short sample = buffer[index];
            output.write(sample & 0xFF);
            output.write(sample >>> 8 & 0xFF);
        }
    }

    private void verifySize(Path path, VoiceDecodeLimits limits) throws VoiceDecodeException {
        try {
            if (Files.size(path) > limits.maximumFileBytes()) {
                throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LARGE,
                        "MP3 exceeds configured file size.");
            }
        } catch (IOException exception) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "MP3 size could not be read.", exception);
        }
    }

    private void rejectOversizePcm(int size, int rate, int channels, VoiceDecodeLimits limits)
            throws VoiceDecodeException {
        if (rate > 0 && channels > 0 && (long) size * 1000L > limits.maximumDurationMillis()
                * rate * channels * Short.BYTES) {
            throw new VoiceDecodeException(VoiceDiagnosticCode.VOICE_ASSET_TOO_LONG,
                    "MP3 exceeds configured duration.");
        }
    }
}
