package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoryDiagnosticDisplayTest {
    @Test
    void syntaxErrorIncludesDatapackPathCoordinatesAndRussianMessage() {
        StoryDiagnostic diagnostic = new StoryDiagnostic(
                "ESE-SOURCE-001",
                DiagnosticSeverity.ERROR,
                "source",
                "Missing semicolon.",
                new DiagnosticSubject("", "", "", "", "jdh:echo_story/stories/chapter_1/main.es", "7:3"),
                Map.of(
                        "sourceId", "jdh:echo_story/stories/chapter_1/main.es",
                        "line", "7",
                        "column", "3",
                        "languageCode", "ESL-PARSE-003"),
                "source-syntax",
                Instant.EPOCH,
                "");

        String line = new StoryDiagnosticDisplay().error(diagnostic);

        assertTrue(line.contains("data/jdh/echo_story/stories/chapter_1/main.es"));
        assertTrue(line.contains("Строка: 7"));
        assertTrue(line.contains("Колонка: 3"));
        assertTrue(line.contains("Пропущена точка с запятой"));
    }

    @Test
    void unknownIntrinsicIsRussianAndIncludesName() {
        StoryDiagnostic diagnostic = new StoryDiagnostic(
                "ESE-SOURCE-021",
                DiagnosticSeverity.ERROR,
                "source",
                "Story source uses an unknown intrinsic",
                new DiagnosticSubject("", "", "", "", "jdh:echo_story/stories/chapter_1/main.es", "11:9"),
                Map.of(
                        "sourceId", "jdh:echo_story/stories/chapter_1/main.es",
                        "line", "11",
                        "column", "9",
                        "intrinsic", "Effect.ApplyToPlayer"),
                "unknown-intrinsic",
                Instant.EPOCH,
                "");

        String line = new StoryDiagnosticDisplay().error(diagnostic);

        assertTrue(line.contains("неизвестную DSL-функцию: Effect.ApplyToPlayer"));
        assertTrue(line.contains("Строка: 11"));
    }
}
