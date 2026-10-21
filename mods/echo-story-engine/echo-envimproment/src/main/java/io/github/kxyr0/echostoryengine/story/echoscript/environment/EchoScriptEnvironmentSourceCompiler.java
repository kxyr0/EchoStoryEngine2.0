package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.LanguageVersion;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentSourceCompiler;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentSourceInput;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.validation.EnvironmentResourceValidator;
import io.github.kxyr0.echostoryengine.story.definition.environment.validation.EnvironmentValidationResult;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * EchoScript source compiler for EnvironmentResource definitions.
 */
public final class EchoScriptEnvironmentSourceCompiler implements EnvironmentSourceCompiler {
    private final EchoScriptParser parser;
    private final LanguageVersion languageVersion;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter = new EchoScriptDiagnosticAdapter();
    private final EnvironmentResourceBinder binder = new EnvironmentResourceBinder();
    private final EnvironmentResourceValidator validator = new EnvironmentResourceValidator();

    public EchoScriptEnvironmentSourceCompiler() {
        this(EchoScriptLanguage.parser(), LanguageVersion.V1);
    }

    EchoScriptEnvironmentSourceCompiler(EchoScriptParser parser, LanguageVersion languageVersion) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.languageVersion = Objects.requireNonNull(languageVersion, "languageVersion");
    }

    @Override
    public EnvironmentCompilationResult compile(EnvironmentSourceInput input, Instant occurredAt) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(occurredAt, "occurredAt");
        ParseResult parse = parser.parse(new ParseRequest(
                languageVersion,
                new SourceId(input.sourceId()),
                input.content(),
                ProcessingLimits.defaults()));
        List<StoryDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parse, occurredAt));
        if (!parse.successful()) {
            return result(new ResultSeed(false, false, false, input.sourceId(), Optional.empty(), diagnostics));
        }
        EnvironmentResourceBinding binding = binder.bind(
                parse.syntaxTree().orElseThrow(), input.sourceId(), occurredAt);
        diagnostics.addAll(binding.diagnostics());
        if (!binding.succeeded()) {
            return result(new ResultSeed(true, false, false, input.sourceId(), Optional.empty(), diagnostics));
        }
        return validate(input.sourceId(), binding.resource().orElseThrow(), diagnostics, occurredAt);
    }

    private EnvironmentCompilationResult validate(
            String sourceId,
            EnvironmentResourceDef resource,
            List<StoryDiagnostic> diagnostics,
            Instant occurredAt) {
        EnvironmentValidationResult validation = validator.validate(resource, sourceId, occurredAt);
        diagnostics.addAll(validation.diagnostics());
        return result(new ResultSeed(true, true, validation.succeeded(), sourceId,
                validation.succeeded() ? Optional.of(resource) : Optional.empty(), diagnostics));
    }

    private EnvironmentCompilationResult result(ResultSeed seed) {
        boolean hasErrors = seed.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
        Optional<EnvironmentResourceDef> accepted = hasErrors ? Optional.empty() : seed.resource();
        return new EnvironmentCompilationResult(seed.parsed(), seed.bound(), seed.validated() && !hasErrors,
                seed.sourceId(), accepted, seed.diagnostics());
    }

    private record ResultSeed(
            boolean parsed,
            boolean bound,
            boolean validated,
            String sourceId,
            Optional<EnvironmentResourceDef> resource,
            List<StoryDiagnostic> diagnostics) {
    }
}
