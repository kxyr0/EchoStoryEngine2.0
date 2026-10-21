package io.github.kxyr0.echovoice.forge.reload.stability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/** Bounded size/mtime stabilization performed on a decode worker. */
public final class VoiceFileStabilityProbe {
    private final int checks;
    private final int retries;
    private final long intervalMillis;

    public VoiceFileStabilityProbe(int checks, int retries, long intervalMillis) {
        this.checks = checks;
        this.retries = retries;
        this.intervalMillis = intervalMillis;
    }

    public boolean stable(Path path) {
        for (int retry = 0; retry < retries; retry++) {
            if (stableAttempt(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean stableAttempt(Path path) {
        try {
            long size = Files.size(path);
            FileTime modified = Files.getLastModifiedTime(path);
            for (int check = 0; check < checks; check++) {
                Thread.sleep(intervalMillis);
                long nextSize = Files.size(path);
                FileTime nextModified = Files.getLastModifiedTime(path);
                if (size != nextSize || !modified.equals(nextModified)) {
                    return false;
                }
            }
            return size > 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
