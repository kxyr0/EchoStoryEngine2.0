package io.github.kxyr0.echodialogue.api.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DiagnosticSubjectTest {
    @Test
    void sourceSubjectCarriesOneBasedCoordinates() {
        DiagnosticSubject subject = DiagnosticSubject.source("sample:broken.dialogue.es", 3, 17).orElseThrow();

        assertEquals("source", subject.kind());
        assertEquals("sample:broken.dialogue.es", subject.value());
        assertEquals(3, subject.line().orElseThrow());
        assertEquals(17, subject.column().orElseThrow());
    }

    @Test
    void sourceCoordinatesMustBeOneBased() {
        assertThrows(IllegalArgumentException.class,
                () -> DiagnosticSubject.source("sample:broken.dialogue.es", 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> DiagnosticSubject.source("sample:broken.dialogue.es", 1, 0));
    }
}
