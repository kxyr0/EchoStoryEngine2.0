package io.github.kxyr0.echodialogue.forge.diagnostic;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import org.slf4j.Logger;

/**
 * Sends structured diagnostics to the standard Forge log pipeline and bounded command buffer.
 */
public final class ForgeDiagnosticSink {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final InMemoryDiagnosticBuffer buffer;

    public ForgeDiagnosticSink(InMemoryDiagnosticBuffer buffer) {
        this.buffer = buffer;
    }

    public void accept(DialogueDiagnostic diagnostic) {
        buffer.accept(diagnostic);
        String message = subject(diagnostic) + diagnostic.message();
        if (diagnostic.severity() == DiagnosticSeverity.ERROR) {
            LOGGER.error("[{}] {}", diagnostic.code(), message);
        } else if (diagnostic.severity() == DiagnosticSeverity.WARNING) {
            LOGGER.warn("[{}] {}", diagnostic.code(), message);
        } else {
            LOGGER.info("[{}] {}", diagnostic.code(), message);
        }
    }

    private String subject(DialogueDiagnostic diagnostic) {
        return diagnostic.subject().map(this::subject).orElse("");
    }

    private String subject(DiagnosticSubject subject) {
        String location = subject.line()
                .map(line -> ":" + line + subject.column().map(column -> ":" + column).orElse(""))
                .orElse("");
        return subject.kind() + " " + subject.value() + location + " - ";
    }
}
