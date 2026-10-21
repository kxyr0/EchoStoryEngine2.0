package io.github.kxyr0.echoaudio.pcm;

import io.github.kxyr0.echoaudio.asset.AudioAssetDescriptor;

@FunctionalInterface
public interface AudioDecoder {
    AudioPcm decode(AudioAssetDescriptor asset) throws Exception;
}
