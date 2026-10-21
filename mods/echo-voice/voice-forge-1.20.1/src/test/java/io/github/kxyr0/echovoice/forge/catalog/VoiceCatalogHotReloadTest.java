package io.github.kxyr0.echovoice.forge.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echovoice.api.asset.VoiceAssetFormat;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceDiagnosticBuffer;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VoiceCatalogHotReloadTest {
    @TempDir
    Path gameDirectory;
    private FilesystemVoiceAssetCatalog catalog;
    private VoiceDiagnosticBuffer diagnostics;
    private VoiceCatalogScanner scanner;
    private Path folder;

    @BeforeEach
    void setUp() throws Exception {
        catalog = new FilesystemVoiceAssetCatalog();
        diagnostics = new VoiceDiagnosticBuffer(16);
        VoiceAssetPathResolver resolver = new VoiceAssetPathResolver(gameDirectory);
        scanner = new VoiceCatalogScanner(resolver, catalog, new VoiceLogSink(diagnostics));
        folder = resolver.dialogueFolder(DialogueId.parse("jdh:first_morning"));
        Files.createDirectories(folder);
    }

    @Test
    void mp3ToWavKeepsIdentityAndIncrementsRevision() throws Exception {
        VoiceAssetKey key = VoiceAssetKey.of(DialogueId.parse("jdh:first_morning"), "john_001");
        Files.write(folder.resolve("john_001.mp3"), new byte[] {1});
        scanner.scan();
        var first = catalog.find(key).orElseThrow();
        Files.delete(folder.resolve("john_001.mp3"));
        Files.write(folder.resolve("john_001.wav"), new byte[] {2});

        scanner.scan();
        var second = catalog.find(key).orElseThrow();

        assertEquals(VoiceAssetFormat.MP3, first.format());
        assertEquals(VoiceAssetFormat.WAV, second.format());
        assertEquals(first.revision().value() + 1, second.revision().value());
        assertEquals(first.key(), second.key());
    }

    @Test
    void ambiguousReplacementRetainsLastKnownGood() throws Exception {
        VoiceAssetKey key = VoiceAssetKey.of(DialogueId.parse("jdh:first_morning"), "john_001");
        Files.write(folder.resolve("john_001.wav"), new byte[] {1});
        scanner.scan();
        var knownGood = catalog.find(key).orElseThrow();
        Files.write(folder.resolve("john_001.mp3"), new byte[] {2});

        scanner.scan();

        assertEquals(knownGood, catalog.find(key).orElseThrow());
        assertTrue(diagnostics.recent().stream()
                .anyMatch(value -> value.code() == VoiceDiagnosticCode.VOICE_ASSET_FORMAT_AMBIGUOUS));
    }

    @Test
    void dialogueTraversalCannotEscapeVoiceRoot() {
        VoiceAssetPathResolver resolver = new VoiceAssetPathResolver(gameDirectory);

        assertThrows(IllegalArgumentException.class,
                () -> resolver.dialogueFolder(DialogueId.parse("jdh:chapter/../outside")));
    }
}
