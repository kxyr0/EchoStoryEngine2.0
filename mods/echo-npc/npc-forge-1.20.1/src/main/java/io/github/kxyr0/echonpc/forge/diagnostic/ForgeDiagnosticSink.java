package io.github.kxyr0.echonpc.forge.diagnostic;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnosticSeverity;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Routes structured Echo NPC diagnostics into the standard Forge logging pipeline and bounded memory buffer.
 */
public final class ForgeDiagnosticSink implements DiagnosticSink {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final InMemoryDiagnosticBuffer buffer;

    public ForgeDiagnosticSink(InMemoryDiagnosticBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
    }

    @Override
    public void accept(NpcDiagnostic diagnostic) {
        buffer.add(diagnostic);
        String text = diagnostic.code() + " " + diagnostic.message() + " " + diagnostic.context();
        if (diagnostic.severity() == NpcDiagnosticSeverity.ERROR) {
            LOGGER.warn(text);
        } else if (diagnostic.severity() == NpcDiagnosticSeverity.WARNING) {
            LOGGER.warn(text);
        } else {
            LOGGER.info(text);
        }
    }

    public InMemoryDiagnosticBuffer buffer() {
        return buffer;
    }
}
