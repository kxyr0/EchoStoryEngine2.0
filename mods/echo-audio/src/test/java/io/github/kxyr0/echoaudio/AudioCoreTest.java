package io.github.kxyr0.echoaudio;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.asset.*;
import io.github.kxyr0.echoaudio.pcm.*;
import io.github.kxyr0.echoaudio.playback.AudioEnvelope;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AudioCoreTest {
    @TempDir Path root;
    Path asset(String name, String value) throws Exception {
        Path path = root.resolve("jdh/sounds/" + name); Files.createDirectories(path.getParent());
        return Files.writeString(path, value);
    }
    AudioPcm pcm(int marker) { return new AudioPcm(new byte[]{(byte) marker, 0, (byte) marker, 0}, 8000, 1); }
    @Test void nestedIdsAndAmbiguityAreDeterministic() throws Exception {
        asset("doors/knock.wav", "a"); asset("doors/knock.mp3", "b"); asset("rain.ogg", "c"); asset("bad.flac", "d");
        var scan = new AudioAssetScanner().scan(root);
        assertEquals(1, scan.assets().size()); assertEquals("jdh:rain", scan.assets().get(0).id().value());
        var ambiguity = scan.problems().stream().filter(f -> f.code().equals("AUDIO_ASSET_AMBIGUOUS")).findFirst().orElseThrow();
        assertTrue(ambiguity.getMessage().contains("knock.wav")); assertTrue(ambiguity.getMessage().contains("knock.mp3"));
        assertTrue(scan.problems().stream().anyMatch(f -> f.code().equals("AUDIO_FORMAT_UNSUPPORTED")));
    }
    @Test void nestedIdIsExtensionFreeAndRevisionTracksBytes() throws Exception {
        Path path = asset("doors/heavy_knock.wav", "a"); var a = new AudioAssetScanner().scan(root).assets().get(0);
        Files.writeString(path, "b"); var b = new AudioAssetScanner().scan(root).assets().get(0);
        assertEquals("jdh:doors/heavy_knock", a.id().value()); assertNotEquals(a.revision(), b.revision());
    }
    @Test void runtimeRejectsAmbiguousAssetInsteadOfChoosingAnExtension() throws Exception {
        asset("knock.wav", "1"); asset("knock.mp3", "2");
        try (var catalog = new AudioCatalog(root, a -> pcm(1), 100)) {
            catalog.reload().join();
            var failure = assertThrows(CompletionException.class,
                    () -> catalog.preload(new AudioResourceId("jdh:knock")).join());
            assertEquals("AUDIO_ASSET_AMBIGUOUS", ((AudioFailure) failure.getCause()).code());
        }
    }
    @Test void cacheDecodesOnceAndOnWorkerEvenForConcurrentRequests() throws Exception {
        asset("knock.wav", "a"); AtomicInteger calls = new AtomicInteger(); String caller = Thread.currentThread().getName();
        try (var catalog = new AudioCatalog(root, a -> {
            assertNotEquals(caller, Thread.currentThread().getName()); calls.incrementAndGet(); return pcm(1);
        }, 100)) {
            catalog.reload().join(); var id = new AudioResourceId("jdh:knock");
            CompletableFuture<?>[] requests = new CompletableFuture<?>[100];
            for (int i = 0; i < requests.length; i++) requests[i] = catalog.preload(id);
            CompletableFuture.allOf(requests).join(); assertEquals(1, calls.get());
        }
    }
    @Test void reloadPinsOldSamplesAndCorruptReplacementKeepsCachedLkg() throws Exception {
        Path path = asset("knock.wav", "1");
        try (var catalog = new AudioCatalog(root, a -> {
            String text = Files.readString(a.path()); if (text.equals("bad")) throw new Exception("corrupt");
            return pcm(Integer.parseInt(text));
        }, 100)) {
            var id = new AudioResourceId("jdh:knock"); catalog.reload().join(); var a = catalog.preload(id).join();
            Files.writeString(path, "2"); catalog.reload().join(); var b = catalog.preload(id).join();
            assertEquals(1, a.local().cursor().get(0)); assertEquals(2, b.local().cursor().get(0));
            Files.writeString(path, "bad"); catalog.reload().join(); assertSame(b, catalog.preload(id).join());
            assertEquals("AUDIO_DECODE_FAILED", catalog.problems().get(0).code());
        }
    }
    @Test void cacheBudgetEvictsWithoutInvalidatingPinnedPlayback() throws Exception {
        asset("a.wav", "1"); asset("b.wav", "2"); AtomicInteger calls = new AtomicInteger();
        try (var catalog = new AudioCatalog(root, a -> { calls.incrementAndGet(); return pcm(3); }, 4)) {
            catalog.reload().join(); var pinned = catalog.preload(new AudioResourceId("jdh:a")).join();
            catalog.preload(new AudioResourceId("jdh:b")).join(); catalog.preload(new AudioResourceId("jdh:a")).join();
            assertEquals(3, calls.get()); assertEquals(3, pinned.local().cursor().get(0));
        }
    }
    @Test void mutationBetweenScanAndDecodeCannotPoisonRevisionCache() throws Exception {
        Path file = asset("knock.wav", "1");
        try (var catalog = new AudioCatalog(root, a -> pcm(1), 100)) {
            catalog.reload().join(); Files.writeString(file, "2");
            var failure = assertThrows(CompletionException.class,
                    () -> catalog.preload(new AudioResourceId("jdh:knock")).join());
            assertEquals("AUDIO_DECODE_FAILED", ((AudioFailure) failure.getCause()).code());
        }
    }
    @Test void monoDownmixPreservesFramesAndCentersChannels() {
        AudioPcm stereo = new AudioPcm(new byte[]{100, 0, (byte) 200, 0}, 8000, 2);
        var mono = stereo.mono(); assertEquals(1, mono.channels()); assertEquals(150, mono.cursor().get(0) & 255);
    }
    @Test void timedLoopFadesAndStopsAtGameplayDeadlineRegardlessOfPitch() {
        for (double pitch : new double[]{.75, 1, 1.25}) {
            var envelope = new AudioEnvelope(new AudioOptions(.8, pitch, 60, 10, 10));
            assertEquals(0, envelope.gain());
            for (int i = 0; i < 50; i++) envelope.tick();
            assertFalse(envelope.finished()); assertEquals(.8, envelope.gain(), .001);
            for (int i = 0; i < 10; i++) envelope.tick();
            assertTrue(envelope.finished()); assertEquals(0, envelope.gain());
        }
    }
    @Test void infiniteLoopAndExplicitStopFadeDoNotChangeVolumeTwice() {
        var envelope = new AudioEnvelope(new AudioOptions(.5, 1, 0, 0, 0));
        for (int i = 0; i < 100; i++) envelope.tick(); assertFalse(envelope.finished());
        envelope.stop(10); assertEquals(.5, envelope.gain());
        for (int i = 0; i < 5; i++) envelope.tick(); assertEquals(.25, envelope.gain());
        for (int i = 0; i < 5; i++) envelope.tick(); assertTrue(envelope.finished());
    }
    @Test void invalidParametersAreRejectedWithoutClamping() {
        for (double volume : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 2}) {
            assertEquals("AUDIO_VOLUME_INVALID", assertThrows(AudioFailure.class,
                    () -> new AudioOptions(volume, 1, 0, 0, 0)).code());
        }
        assertThrows(AudioFailure.class, () -> new AudioOptions(1, 3, 0, 0, 0));
        assertThrows(AudioFailure.class, () -> new AudioOptions(1, 1, -1, 0, 0));
        assertThrows(AudioFailure.class, () -> new AudioOptions(1, 1, 10, 20, 0));
    }
}
