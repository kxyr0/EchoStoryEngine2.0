package io.github.kxyr0.echovoice.forge.reload.stability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VoiceFileStabilityProbeTest {
    @TempDir
    Path temporary;

    @Test
    void nonEmptyStableFilePassesBoundedChecks() throws Exception {
        Path candidate = temporary.resolve("voice.wav");
        Files.write(candidate, new byte[] {1, 2, 3});

        assertTrue(new VoiceFileStabilityProbe(2, 2, 1).stable(candidate));
    }

    @Test
    void zeroByteAndMissingCandidatesNeverCommit() throws Exception {
        Path empty = temporary.resolve("empty.wav");
        Files.createFile(empty);
        VoiceFileStabilityProbe probe = new VoiceFileStabilityProbe(1, 1, 1);

        assertFalse(probe.stable(empty));
        assertFalse(probe.stable(temporary.resolve("missing.wav")));
    }
}
