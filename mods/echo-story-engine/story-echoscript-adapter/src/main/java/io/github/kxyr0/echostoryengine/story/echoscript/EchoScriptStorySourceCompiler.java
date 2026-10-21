package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.LanguageVersion;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.definition.compile.SourceStoryCompiler;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompileResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.StorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.definition.validation.SemanticStoryValidator;
import io.github.kxyr0.echostoryengine.story.definition.validation.StoryValidationResult;
import io.github.kxyr0.echostoryengine.story.definition.validation.StructuralStoryValidator;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.StoryResourceBinder;
import io.github.kxyr0.echostoryengine.story.echoscript.binding.StoryResourceBinding;
import io.github.kxyr0.echostoryengine.story.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Story-owned source compiler that consumes EchoScript syntax and binds the StoryResource profile.
 */
public final class EchoScriptStorySourceCompiler implements StorySourceCompiler {
    private final EchoScriptParser parser;
    private final LanguageVersion languageVersion;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter = new EchoScriptDiagnosticAdapter();
    private final StoryResourceBinder binder = new StoryResourceBinder();
    private final StructuralStoryValidator structuralValidator = new StructuralStoryValidator();
    private final SemanticStoryValidator semanticValidator = new SemanticStoryValidator();
    private final SourceStoryCompiler compiler = new SourceStoryCompiler();

    public EchoScriptStorySourceCompiler() {
        this(EchoScriptLanguage.parser(), LanguageVersion.V1);
    }

    public EchoScriptStorySourceCompiler(int languageMajor) {
        this(EchoScriptLanguage.parser(), LanguageVersion.of(languageMajor));
    }

    EchoScriptStorySourceCompiler(EchoScriptParser parser, LanguageVersion languageVersion) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.languageVersion = Objects.requireNonNull(languageVersion, "languageVersion");
    }

    @Override
    public StoryCompilationResult compile(StorySourceInput input, Instant occurredAt) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(occurredAt, "occurredAt");
        ParseResult parse = parser.parse(new ParseRequest(
                languageVersion,
                new SourceId(input.sourceId()),
                input.content(),
                ProcessingLimits.defaults()));
        List<StoryDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parse, occurredAt));
        if (!parse.successful()) {
            return result(new ResultParts(
                    false,
                    false,
                    false,
                    false,
                    input.sourceId(),
                    Optional.empty(),
                    Optional.empty(),
                    diagnostics));
        }
        StoryResourceBinding binding = binder.bind(parse.syntaxTree().orElseThrow(), input.sourceId(), occurredAt);
        diagnostics.addAll(binding.diagnostics());
        if (!binding.succeeded()) {
            return result(new ResultParts(
                    true,
                    false,
                    false,
                    false,
                    input.sourceId(),
                    Optional.empty(),
                    Optional.empty(),
                    diagnostics));
        }
        return validateAndCompile(binding.sourceStory().orElseThrow(), diagnostics, occurredAt);
    }

    private StoryCompilationResult validateAndCompile(
            SourceStory source,
            List<StoryDiagnostic> diagnostics,
            Instant occurredAt) {
        StoryValidationResult structural = structuralValidator.validate(source, occurredAt);
        diagnostics.addAll(structural.diagnostics());
        if (!structural.succeeded()) {
            return result(new ResultParts(
                    true,
                    false,
                    false,
                    false,
                    source.sourceId(),
                    source.formatVersion(),
                    Optional.empty(),
                    diagnostics));
        }
        StoryValidationResult semantic = semanticValidator.validate(source, occurredAt);
        diagnostics.addAll(semantic.diagnostics());
        if (!semantic.succeeded()) {
            return result(new ResultParts(
                    true,
                    true,
                    false,
                    false,
                    source.sourceId(),
                    source.formatVersion(),
                    Optional.empty(),
                    diagnostics));
        }
        StoryCompileResult compile = compiler.compile(source, occurredAt);
        diagnostics.addAll(compile.diagnostics());
        return result(new ResultParts(
                true,
                true,
                true,
                compile.succeeded(),
                source.sourceId(),
                source.formatVersion(),
                compile.compiledStory(),
                diagnostics));
    }

    private StoryCompilationResult result(ResultParts parts) {
        boolean hasErrors = parts.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
        return new StoryCompilationResult(
                parts.parsing(),
                parts.structural(),
                parts.semantic(),
                parts.compilation() && !hasErrors,
                parts.sourceId(),
                parts.formatVersion(),
                parts.compilation() && !hasErrors ? parts.story() : Optional.empty(),
                parts.diagnostics());
    }

    private record ResultParts(
            boolean parsing,
            boolean structural,
            boolean semantic,
            boolean compilation,
            String sourceId,
            Optional<Integer> formatVersion,
            Optional<StoryDefinition> story,
            List<StoryDiagnostic> diagnostics) {
    }
}
