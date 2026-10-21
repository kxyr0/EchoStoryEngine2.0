package io.github.kxyr0.echoaudio.asset;

import io.github.kxyr0.echoaudio.api.AudioResourceId;
import io.github.kxyr0.echoaudio.api.AudioRevision;
import java.nio.file.Path;

public record AudioAssetDescriptor(AudioResourceId id, AudioRevision revision, Path path, String format) { }
