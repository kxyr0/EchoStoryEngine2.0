package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.diagnostic.InspectionDiagnostic;
import io.github.kxyr0.echoiteminspection.diagnostic.ItemSuggestions;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Parses once, binds strictly, and rejects absent Minecraft/Echo registry items before interaction. */
public final class InspectionCompiler {
    public InspectionCompileResult compile(InspectionSource source, ItemInspectionDefinitionRevision revision,
            Set<String> knownItemIds) {
        InspectionCompileResult compiled = compile(source, revision);
        if (!compiled.successful()) return compiled;
        var definition = compiled.definition().orElseThrow();
        if (knownItemIds.contains(definition.itemId())) return compiled;
        List<InspectionDiagnostic> diagnostics = new ArrayList<>(compiled.diagnostics());
        diagnostics.add(new InspectionDiagnostic("ITEM_INSPECTION_ITEM_NOT_FOUND",
                "Item \"" + definition.itemId() + "\" not found.", definition.descriptor().itemSpan(), true,
                ItemSuggestions.find(definition.itemId(), knownItemIds)));
        return invalid(diagnostics);
    }

    /** Produces semantic descriptors for offline tools; the catalog binds registry IDs separately. */
    public InspectionCompileResult compile(InspectionSource source, ItemInspectionDefinitionRevision revision) {
        SourcePosition start = new SourcePosition(0, 1, 1);
        SourceSpan fallback = new SourceSpan(new SourceId(source.sourceId()), 0, 0, start, start);
        List<InspectionDiagnostic> diagnostics = new ArrayList<>();
        try {
            ParseResult parse = parse(source, fallback);
            parse.diagnostics().forEach(value -> diagnostics.add(new InspectionDiagnostic(value.code(), value.message(),
                    value.span().orElse(fallback), value.severity() == DiagnosticSeverity.ERROR, List.of())));
            if (!parse.successful() || parse.syntaxTree().isEmpty()) return invalid(diagnostics);
            var definition = new InspectionDefinitionBinder().bind(parse.syntaxTree().orElseThrow(), source, revision);
            return new InspectionCompileResult(Optional.of(definition), diagnostics);
        } catch (BindingFailure failure) {
            diagnostics.add(failure.diagnostic());
        } catch (IllegalArgumentException failure) {
            diagnostics.add(InspectionDiagnostic.error("ITEM_INSPECTION_INVALID_RESOURCE", failure.getMessage(), fallback));
        }
        return invalid(diagnostics);
    }

    private ParseResult parse(InspectionSource source, SourceSpan fallback) {
        try {
            return EchoScriptLanguage.parser().parse(ParseRequest.ofV1(fallback.sourceId(), source.text()));
        } catch (RuntimeException failure) {
            throw new BindingFailure("ITEM_INSPECTION_SYNTAX_ERROR",
                    "EchoScript could not parse source: " + failure.getClass().getSimpleName() + ": "
                            + String.valueOf(failure.getMessage()), fallback);
        }
    }

    private InspectionCompileResult invalid(List<InspectionDiagnostic> diagnostics) {
        return new InspectionCompileResult(Optional.empty(), diagnostics);
    }
}
