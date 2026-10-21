package io.github.kxyr0.echocontentdoctor.provider.audio;

import io.github.kxyr0.echoaudio.asset.AudioAssetScanner;
import io.github.kxyr0.echocontentdoctor.model.*;
import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import java.nio.file.*;
import java.util.*;

/** Adapts authoritative audio descriptors; does not parse audio files or script syntax. */
public final class AudioDescriptorProvider {
    public void append(List<Path> roots, List<ContentDescriptor> descriptors, List<ContentDiagnostic> diagnostics)
            throws java.io.IOException {
        Set<Path> scanned = new HashSet<>();
        for (Path root : roots) {
            Path pack = Files.isDirectory(root.resolve("sounds")) ? root.getParent() : root;
            if (pack == null || !scanned.add(pack)) continue;
            var scan = new AudioAssetScanner().scan(pack);
            for (var asset : scan.assets()) {
                descriptors.add(ContentDescriptor.simple(new ContentResourceId(asset.id().value()), ContentResourceKind.AUDIO,
                        SourceSpans.file(asset.path().toString(), ""), List.of()));
            }
            for (var failure : scan.problems()) {
                diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR, failure.code(),
                        new ContentResourceId("echo_audio:catalog"), SourceSpans.file(pack.toString(), ""),
                        failure.getMessage(), List.of()));
            }
        }
    }
}
