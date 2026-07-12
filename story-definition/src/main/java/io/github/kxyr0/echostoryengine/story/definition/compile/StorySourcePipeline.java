package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.definition.parse.JsonStorySourceParser;
import io.github.kxyr0.echostoryengine.story.definition.parse.StoryParseResult;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.definition.validation.SemanticStoryValidator;
import io.github.kxyr0.echostoryengine.story.definition.validation.StoryValidationResult;
import io.github.kxyr0.echostoryengine.story.definition.validation.StructuralStoryValidator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * End-to-end source pipeline from input text to optional compiled story.
 */
public final class StorySourcePipeline {
    private final JsonStorySourceParser parser = new JsonStorySourceParser();
    private final StructuralStoryValidator structuralValidator = new StructuralStoryValidator();
    private final SemanticStoryValidator semanticValidator = new SemanticStoryValidator();
    private final StorySourceCompiler compiler = new StorySourceCompiler();

    public StoryCompilationResult compile(StorySourceInput input, Instant occurredAt) {
        StoryParseResult parse = parser.parse(input, occurredAt);
        List<StoryDiagnostic> diagnostics = new ArrayList<>(parse.diagnostics());
        if (!parse.succeeded()) {
            return result(parse, StageStatus.none(), Optional.empty(), diagnostics);
        }
        SourceStory source = parse.source().orElseThrow();
        StoryValidationResult structural = structuralValidator.validate(source, occurredAt);
        diagnostics.addAll(structural.diagnostics());
        if (!structural.succeeded()) {
            return result(parse, StageStatus.none(), Optional.empty(), diagnostics);
        }
        return compileAfterStructural(parse, source, occurredAt, diagnostics);
    }

    private StoryCompilationResult compileAfterStructural(
            StoryParseResult parse,
            SourceStory source,
            Instant occurredAt,
            List<StoryDiagnostic> diagnostics) {
        StoryValidationResult semantic = semanticValidator.validate(source, occurredAt);
        diagnostics.addAll(semantic.diagnostics());
        if (!semantic.succeeded()) {
            return result(parse, StageStatus.structuralOnly(), Optional.empty(), diagnostics);
        }
        StoryCompileResult compile = compiler.compile(source, occurredAt);
        diagnostics.addAll(compile.diagnostics());
        StageStatus status = new StageStatus(true, true, compile.succeeded());
        return result(parse, status, compile.compiledStory(), diagnostics);
    }

    private StoryCompilationResult result(
            StoryParseResult parse,
            StageStatus status,
            Optional<StoryDefinition> compiledStory,
            List<StoryDiagnostic> diagnostics) {
        return new StoryCompilationResult(
                parse.succeeded(),
                status.structuralSucceeded(),
                status.semanticSucceeded(),
                status.compilationSucceeded(),
                parse.sourceId(),
                parse.formatVersion(),
                compiledStory,
                diagnostics);
    }

    private record StageStatus(
            boolean structuralSucceeded,
            boolean semanticSucceeded,
            boolean compilationSucceeded) {
        static StageStatus none() {
            return new StageStatus(false, false, false);
        }

        static StageStatus structuralOnly() {
            return new StageStatus(true, false, false);
        }
    }
}
