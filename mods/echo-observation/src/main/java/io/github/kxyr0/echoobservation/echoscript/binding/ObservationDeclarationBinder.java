package io.github.kxyr0.echoobservation.echoscript.binding;

import io.github.kxyr0.echoobservation.api.action.ObservationScriptBlock;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic.Severity;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationLocalId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationDuration;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import io.github.kxyr0.echoobservation.api.model.ObservationSource;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ObservationDeclarationBinder {
    private final ObservationActionBinder actions = new ObservationActionBinder();

    ObservationDefinition bind(ObservationResourceId resourceId, MethodSyntax method, String sourceId,
            long resourceRevision, List<ObservationDiagnostic> diagnostics) {
        Values values = new Values();
        method.body().statements().forEach(statement -> read(statement, values, diagnostics));
        String local = ObservationNameNormalizer.normalize(method.name());
        if (local.isBlank()) {
            error(diagnostics, method.span().startOffset(), "Observation declaration name cannot form an ID.");
            local = "invalid";
        }
        if (values.anchor.isEmpty()) {
            diagnostics.add(new ObservationDiagnostic(ObservationDiagnosticCode.OBSERVATION_UNPLACED,
                    Severity.HINT, "Observation is unplaced and unavailable in gameplay.", method.span().startOffset()));
        }
        ObservationSource source = new ObservationSource(sourceId, method.span().startOffset(),
                method.span().endOffset(), method.body().span().startOffset(), values.atSpan,
                method.span().start().line());
        return new ObservationDefinition(new ObservationId(resourceId, new ObservationLocalId(local)), method.name(),
                values.anchor, values.focus, values.distance, values.radius, values.repeatable, values.text,
                values.thought, values.onComplete, values.formerIds,
                definitionRevision(resourceRevision, method.name()), source);
    }

    private void read(Object raw, Values values, List<ObservationDiagnostic> diagnostics) {
        if (raw instanceof SectionStatementSyntax section) {
            if (!ObservationCallReader.name(section.invocation()).equals("OnComplete") || values.hasCompletion) {
                error(diagnostics, section.span().startOffset(), "Unknown or duplicate Observation section.");
                return;
            }
            values.hasCompletion = true;
            values.onComplete = actions.bind(section, diagnostics);
            return;
        }
        if (!(raw instanceof ExpressionStatementSyntax statement)
                || !(statement.expression() instanceof InvocationExpressionSyntax call)) {
            error(diagnostics, ((io.github.kxyr0.echoscript.api.syntax.SyntaxNode) raw).span().startOffset(),
                    "Observation body accepts declaration calls only.");
            return;
        }
        switch (ObservationCallReader.name(call)) {
            case "At" -> readAnchor(call, statement, values, diagnostics);
            case "Focus" -> number(call, diagnostics).ifPresent(value -> values.focus = duration(value, call, diagnostics));
            case "Distance" -> number(call, diagnostics).ifPresent(value -> values.distance = value);
            case "ScreenRadius" -> number(call, diagnostics).ifPresent(value -> values.radius = value);
            case "Repeatable" -> bool(call, diagnostics).ifPresent(value -> values.repeatable = value);
            case "Text" -> string(call, diagnostics).ifPresent(value -> values.text = Optional.of(value));
            case "Thought" -> string(call, diagnostics).ifPresent(value -> values.thought = Optional.of(value));
            case "Formerly" -> string(call, diagnostics).ifPresent(value ->
                    former(value, call, values, diagnostics));
            default -> error(diagnostics, call.span().startOffset(), "Unknown Observation declaration call: "
                    + ObservationCallReader.name(call));
        }
    }

    private void readAnchor(InvocationExpressionSyntax call, ExpressionStatementSyntax statement, Values values,
            List<ObservationDiagnostic> diagnostics) {
        if (values.anchor.isPresent()) {
            error(diagnostics, call.span().startOffset(), "Observation declares At more than once.");
            return;
        }
        var block = ObservationCallReader.positional(call, 0).flatMap(value ->
                ObservationCallReader.invocation(value, "Block"));
        var offset = ObservationCallReader.positional(call, 1).flatMap(value ->
                ObservationCallReader.invocation(value, "Offset"));
        var face = ObservationCallReader.named(call, "face").flatMap(ObservationCallReader::scalar);
        var dimension = ObservationCallReader.named(call, "dimension").flatMap(ObservationCallReader::string);
        if (block.isEmpty() || offset.isEmpty() || face.isEmpty() || dimension.isEmpty()) {
            error(diagnostics, call.span().startOffset(), "At requires Block, Offset, face and dimension.");
            return;
        }
        try {
            values.anchor = Optional.of(new ObservationAnchor(dimension.orElseThrow(), integer(block.orElseThrow(), 0),
                    integer(block.orElseThrow(), 1), integer(block.orElseThrow(), 2),
                    decimal(offset.orElseThrow(), 0), decimal(offset.orElseThrow(), 1),
                    decimal(offset.orElseThrow(), 2), ObservationFace.valueOf(face.orElseThrow())));
            values.atSpan = Optional.of(new ObservationSource.Span(statement.span().startOffset(),
                    statement.span().endOffset()));
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            error(diagnostics, call.span().startOffset(), "Invalid At declaration: " + exception.getMessage());
        }
    }

    private int integer(InvocationExpressionSyntax call, int index) {
        return ObservationCallReader.positional(call, index).flatMap(ObservationCallReader::integer).orElseThrow();
    }

    private double decimal(InvocationExpressionSyntax call, int index) {
        return ObservationCallReader.positional(call, index).flatMap(ObservationCallReader::number).orElseThrow();
    }

    private Optional<Double> number(InvocationExpressionSyntax call, List<ObservationDiagnostic> diagnostics) {
        Optional<Double> value = ObservationCallReader.positional(call, 0).flatMap(ObservationCallReader::number);
        if (value.isEmpty()) error(diagnostics, call.span().startOffset(), "Call requires one number.");
        return value;
    }

    private Optional<Boolean> bool(InvocationExpressionSyntax call, List<ObservationDiagnostic> diagnostics) {
        Optional<Boolean> value = ObservationCallReader.positional(call, 0).flatMap(ObservationCallReader::bool);
        if (value.isEmpty()) error(diagnostics, call.span().startOffset(), "Call requires one boolean.");
        return value;
    }

    private Optional<String> string(InvocationExpressionSyntax call, List<ObservationDiagnostic> diagnostics) {
        Optional<String> value = ObservationCallReader.positional(call, 0).flatMap(ObservationCallReader::string);
        if (value.isEmpty()) error(diagnostics, call.span().startOffset(), "Call requires one string.");
        return value;
    }

    private void former(String value, InvocationExpressionSyntax call, Values values,
            List<ObservationDiagnostic> diagnostics) {
        try { values.formerIds.add(new ObservationLocalId(value).value()); }
        catch (IllegalArgumentException exception) {
            error(diagnostics, call.span().startOffset(), exception.getMessage());
        }
    }

    private ObservationDuration duration(double value, InvocationExpressionSyntax call,
            List<ObservationDiagnostic> diagnostics) {
        try {
            return ObservationDuration.seconds(value);
        } catch (IllegalArgumentException exception) {
            error(diagnostics, call.span().startOffset(), exception.getMessage());
            return ObservationDuration.seconds(1.0D);
        }
    }

    private long definitionRevision(long resourceRevision, String symbol) {
        return (31L * resourceRevision + symbol.hashCode()) & Long.MAX_VALUE;
    }

    private void error(List<ObservationDiagnostic> diagnostics, int offset, String message) {
        diagnostics.add(new ObservationDiagnostic(ObservationDiagnosticCode.OBS_DECLARATION_INVALID,
                Severity.ERROR, message, offset));
    }

    private static final class Values {
        private Optional<ObservationAnchor> anchor = Optional.empty();
        private ObservationDuration focus = ObservationDuration.seconds(1.0D);
        private double distance = 6.0D;
        private double radius = 18.0D;
        private boolean repeatable;
        private Optional<String> text = Optional.empty();
        private Optional<String> thought = Optional.empty();
        private ObservationScriptBlock onComplete = ObservationScriptBlock.EMPTY;
        private final List<String> formerIds = new ArrayList<>();
        private Optional<ObservationSource.Span> atSpan = Optional.empty();
        private boolean hasCompletion;
    }
}
