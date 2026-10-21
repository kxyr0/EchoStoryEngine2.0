package io.github.kxyr0.echocontentdoctor.report;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class ContentDoctorJsonReport {
    public void write(Path path, ContentDoctorResult result) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        String diagnostics = result.diagnostics().stream().map(this::diagnostic)
                .collect(Collectors.joining(",\n    "));
        String json = """
                {
                  "revision": %d,
                  "files": %d,
                  "resources": %d,
                  "references": %d,
                  "errors": %d,
                  "warnings": %d,
                  "diagnostics": [
                    %s
                  ]
                }
                """.formatted(result.revision(), result.files(), result.resources(), result.references(),
                result.errors(), result.warnings(), diagnostics);
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String diagnostic(ContentDiagnostic value) {
        String suggestions = value.suggestions().stream().map(id -> "\"" + escape(id.value()) + "\"")
                .collect(Collectors.joining(", "));
        return "{\"severity\":\"" + value.severity() + "\",\"code\":\"" + escape(value.code())
                + "\",\"resource\":\"" + escape(value.resource().value()) + "\",\"source\":\""
                + escape(value.span().sourceId().value()) + "\",\"line\":" + value.span().start().line()
                + ",\"column\":" + value.span().start().column() + ",\"message\":\""
                + escape(value.message()) + "\",\"suggestions\":[" + suggestions + "]}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
