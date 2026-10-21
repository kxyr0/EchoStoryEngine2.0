package io.github.kxyr0.echovoice.forge.diagnostic;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/** Structured, rate-limited logger backed by the queryable diagnostic buffer. */
public final class VoiceLogSink {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long RATE_LIMIT_MILLIS = 2_000L;
    private final VoiceDiagnosticBuffer buffer;
    private final Map<String, Long> lastLogged = new HashMap<>();

    public VoiceLogSink(VoiceDiagnosticBuffer buffer) {
        this.buffer = buffer;
    }

    public synchronized void accept(VoiceDiagnostic diagnostic) {
        buffer.add(diagnostic);
        String key = diagnostic.code() + ":" + diagnostic.assetKey().map(Object::toString).orElse("-");
        long now = System.currentTimeMillis();
        if (now - lastLogged.getOrDefault(key, 0L) < RATE_LIMIT_MILLIS) {
            return;
        }
        lastLogged.put(key, now);
        String message = "[{}] {} asset={}";
        Object asset = diagnostic.assetKey().map(Object::toString).orElse("-");
        switch (diagnostic.severity()) {
            case INFO -> LOGGER.info(message, diagnostic.code(), diagnostic.message(), asset);
            case WARNING -> LOGGER.warn(message, diagnostic.code(), diagnostic.message(), asset);
            case ERROR -> LOGGER.error(message, diagnostic.code(), diagnostic.message(), asset);
        }
    }
}
