package io.github.kxyr0.echoobservation.authoring.core;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import java.util.Locale;

public final class ObservationSourcePatch {
    private final ObservationResourceCompiler compiler = new ObservationResourceCompiler();

    public ObservationPatchResult place(String source, ObservationDefinition selected, ObservationAnchor anchor) {
        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        String indent = statementIndent(source, selected);
        String at = render(anchor, indent, newline);
        String candidate;
        if (selected.source().atSpan().isPresent()) {
            var span = selected.source().atSpan().orElseThrow();
            candidate = source.substring(0, span.start()) + at + source.substring(span.end());
        } else {
            int insertion = selected.source().bodyStart() + 1;
            candidate = source.substring(0, insertion) + newline + indent + at + newline
                    + source.substring(insertion);
        }
        return validate(candidate, selected, anchor, true);
    }

    public ObservationPatchResult clear(String source, ObservationDefinition selected) {
        if (selected.source().atSpan().isEmpty()) return new ObservationPatchResult(java.util.Optional.of(source),
                "Observation is already unplaced.");
        var span = selected.source().atSpan().orElseThrow();
        int start = lineStart(source, span.start());
        if (!source.substring(start, span.start()).isBlank()) start = span.start();
        int end = lineEnd(source, span.end());
        String candidate = source.substring(0, start) + source.substring(end);
        return validate(candidate, selected, null, false);
    }

    private ObservationPatchResult validate(String candidate, ObservationDefinition selected,
            ObservationAnchor anchor, boolean placed) {
        var result = compiler.compile(selected.source().sourceId(), candidate);
        if (!result.successful()) {
            return new ObservationPatchResult(java.util.Optional.empty(), "OBS_AUTHORING_CANDIDATE_INVALID: "
                    + result.diagnostics());
        }
        var definition = result.resource().orElseThrow().observations().stream()
                .filter(value -> value.id().equals(selected.id())).findFirst();
        boolean valid = definition.isPresent() && definition.orElseThrow().placed() == placed
                && (!placed || sameAnchor(definition.orElseThrow().anchor().orElseThrow(), anchor));
        return valid ? new ObservationPatchResult(java.util.Optional.of(candidate), "candidate valid")
                : new ObservationPatchResult(java.util.Optional.empty(), "OBS_AUTHORING_CANDIDATE_INVALID");
    }

    private boolean sameAnchor(ObservationAnchor rendered, ObservationAnchor raycast) {
        return rendered.dimension().equals(raycast.dimension())
                && rendered.blockX() == raycast.blockX()
                && rendered.blockY() == raycast.blockY()
                && rendered.blockZ() == raycast.blockZ()
                && rendered.face() == raycast.face()
                && near(rendered.offsetX(), raycast.offsetX())
                && near(rendered.offsetY(), raycast.offsetY())
                && near(rendered.offsetZ(), raycast.offsetZ());
    }

    private boolean near(double rendered, double raycast) {
        return Math.abs(rendered - raycast) <= 0.000_501D;
    }

    private String render(ObservationAnchor anchor, String indent, String newline) {
        String deeper = indent + "    ";
        return "At(" + newline
                + deeper + "Block(" + anchor.blockX() + ", " + anchor.blockY() + ", " + anchor.blockZ() + ")," + newline
                + deeper + "Offset(" + decimal(anchor.offsetX()) + ", " + decimal(anchor.offsetY()) + ", "
                + decimal(anchor.offsetZ()) + ")," + newline
                + deeper + "face: " + anchor.face() + "," + newline
                + deeper + "dimension: \"" + anchor.dimension() + "\"" + newline
                + indent + ");";
    }

    private String decimal(double value) { return String.format(Locale.ROOT, "%.3f", value); }

    private String statementIndent(String source, ObservationDefinition selected) {
        int start = lineStart(source, selected.source().declarationStart());
        String declarationIndent = source.substring(start, selected.source().declarationStart());
        return declarationIndent.chars().allMatch(value -> value == ' ' || value == '\t')
                ? declarationIndent + "    " : "    ";
    }

    private int lineStart(String source, int offset) {
        int newline = source.lastIndexOf('\n', Math.max(0, offset - 1));
        return newline < 0 ? 0 : newline + 1;
    }

    private int lineEnd(String source, int offset) {
        int newline = source.indexOf('\n', offset);
        return newline < 0 ? source.length() : newline + 1;
    }
}
