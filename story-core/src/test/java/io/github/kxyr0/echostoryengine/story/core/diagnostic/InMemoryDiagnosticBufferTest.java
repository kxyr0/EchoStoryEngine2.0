package io.github.kxyr0.echostoryengine.story.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class InMemoryDiagnosticBufferTest {
    @Test
    void keepsOnlyTheMostRecentDiagnostics() {
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(2);

        buffer.accept(diagnostic("ESE-RUNTIME-001", DiagnosticSeverity.INFO));
        buffer.accept(diagnostic("ESE-RUNTIME-002", DiagnosticSeverity.WARN));
        buffer.accept(diagnostic("ESE-RUNTIME-003", DiagnosticSeverity.ERROR));

        assertEquals(2, buffer.recent().size());
        assertEquals("ESE-RUNTIME-002", buffer.recent().get(0).code());
        assertEquals("ESE-RUNTIME-003", buffer.recent().get(1).code());
    }

    @Test
    void filtersBySeverityAndCode() {
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(4);

        buffer.accept(diagnostic("ESE-RUNTIME-001", DiagnosticSeverity.INFO));
        buffer.accept(diagnostic("ESE-RUNTIME-002", DiagnosticSeverity.ERROR));

        assertEquals(1, buffer.recentBySeverity(DiagnosticSeverity.ERROR).size());
        assertEquals(1, buffer.recentByCode("ESE-RUNTIME-001").size());
    }

    private static StoryDiagnostic diagnostic(String code, DiagnosticSeverity severity) {
        return new StoryDiagnostic(
                code,
                severity,
                "runtime",
                "test diagnostic",
                new DiagnosticSubject("", "", "", "", "test-correlation", ""),
                Map.of(),
                "test",
                Instant.parse("2026-07-11T00:00:00Z"),
                "");
    }
}
