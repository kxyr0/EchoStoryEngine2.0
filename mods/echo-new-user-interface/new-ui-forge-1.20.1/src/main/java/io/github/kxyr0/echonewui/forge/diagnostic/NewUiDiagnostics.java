package io.github.kxyr0.echonewui.forge.diagnostic;

import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;

public final class NewUiDiagnostics {
    private static final long LOG_INTERVAL_MS = 5000L;
    private final Logger logger;
    private final Map<NewUiDiagnosticCode, Long> lastLogAt =
            new EnumMap<>(NewUiDiagnosticCode.class);
    private NewUiDiagnosticCode lastCode;
    private String lastMessage = "OK";

    public NewUiDiagnostics(Logger logger) {
        this.logger = logger;
    }

    public synchronized void warn(NewUiDiagnosticCode code, String detail) {
        record(code, detail);
        if (shouldLog(code)) {
            logger.warn("{}: {} {}", code, code.russianMessage(), detail);
        }
    }

    public synchronized void error(NewUiDiagnosticCode code, String detail, RuntimeException exception) {
        record(code, detail);
        if (shouldLog(code)) {
            logger.error("{}: {} {}", code, code.russianMessage(), detail, exception);
        }
    }

    public synchronized String snapshot() {
        return lastCode == null ? lastMessage : lastCode + ": " + lastMessage;
    }

    private void record(NewUiDiagnosticCode code, String detail) {
        lastCode = code;
        lastMessage = detail == null || detail.isBlank() ? code.russianMessage() : detail;
    }

    private boolean shouldLog(NewUiDiagnosticCode code) {
        long now = System.currentTimeMillis();
        long last = lastLogAt.getOrDefault(code, Long.MIN_VALUE);
        if (now - last < LOG_INTERVAL_MS) {
            return false;
        }
        lastLogAt.put(code, now);
        return true;
    }
}
