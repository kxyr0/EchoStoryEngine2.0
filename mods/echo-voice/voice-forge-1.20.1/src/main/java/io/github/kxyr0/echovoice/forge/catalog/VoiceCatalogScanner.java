package io.github.kxyr0.echovoice.forge.catalog;

import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One startup index pass; audio bytes are not decoded here. */
public final class VoiceCatalogScanner {
    private final VoiceAssetPathResolver resolver;
    private final FilesystemVoiceAssetCatalog catalog;
    private final VoiceLogSink diagnostics;

    public VoiceCatalogScanner(VoiceAssetPathResolver resolver, FilesystemVoiceAssetCatalog catalog,
            VoiceLogSink diagnostics) {
        this.resolver = resolver;
        this.catalog = catalog;
        this.diagnostics = diagnostics;
    }

    public long scan() {
        long started = System.nanoTime();
        groups().forEach(this::indexGroup);
        return elapsedMillis(started);
    }

    public Set<VoiceAssetKey> discoverKeys() {
        return Set.copyOf(groups().keySet());
    }

    private Map<VoiceAssetKey, List<Path>> groups() {
        Path root = resolver.contentPacksRoot();
        if (!Files.isDirectory(root)) {
            return Map.of();
        }
        Map<VoiceAssetKey, List<Path>> grouped = new LinkedHashMap<>();
        try (var paths = Files.find(root, 16, (path, attributes) -> attributes.isRegularFile())) {
            paths.forEach(path -> resolver.keyForPath(path).ifPresent(key -> grouped.merge(key, List.of(path),
                    (left, right) -> java.util.stream.Stream.concat(left.stream(), right.stream()).toList())));
        } catch (IOException exception) {
            diagnostics.accept(VoiceDiagnostic.error(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "Voice catalog scan failed: " + exception.getMessage(), null));
            return Map.of();
        }
        return grouped;
    }

    private void indexGroup(VoiceAssetKey key, List<Path> paths) {
        if (paths.size() != 1) {
            diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_ASSET_FORMAT_AMBIGUOUS,
                    "Several supported files share one stem; none selected.", key));
            return;
        }
        Path path = paths.get(0);
        try {
            VoiceAssetFormat format = VoiceAssetFormat.fromFilename(path.getFileName().toString()).orElseThrow();
            catalog.commit(key, format, path, Files.size(path), 0);
        } catch (IOException exception) {
            diagnostics.accept(VoiceDiagnostic.error(VoiceDiagnosticCode.VOICE_ASSET_READ_FAILED,
                    "Voice asset metadata could not be read.", key));
        }
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
