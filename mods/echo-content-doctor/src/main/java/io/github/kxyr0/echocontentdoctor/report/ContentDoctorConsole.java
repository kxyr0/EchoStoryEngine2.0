package io.github.kxyr0.echocontentdoctor.report;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorResult;

public final class ContentDoctorConsole {
    public String format(ContentDoctorResult result) {
        StringBuilder output = new StringBuilder("Echo Content Doctor\n\n");
        for (ContentDiagnostic diagnostic : result.diagnostics()) {
            output.append(diagnostic.span().sourceId().value()).append(':')
                    .append(diagnostic.span().start().line()).append(':')
                    .append(diagnostic.span().start().column()).append("\n[")
                    .append(diagnostic.severity()).append(' ').append(diagnostic.code()).append("]\n")
                    .append(diagnostic.message()).append('\n');
            if (!diagnostic.suggestions().isEmpty()) {
                output.append("Did you mean:\n");
                diagnostic.suggestions().forEach(value -> output.append("  ").append(value.value()).append('\n'));
            }
            output.append('\n');
        }
        output.append("Files: ").append(result.files()).append('\n')
                .append("Resources: ").append(result.resources()).append('\n')
                .append("Errors: ").append(result.errors()).append('\n')
                .append("Warnings: ").append(result.warnings()).append('\n')
                .append("References: ").append(result.references()).append(" checked\n");
        return output.toString();
    }
}
