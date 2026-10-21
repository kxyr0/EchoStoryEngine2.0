package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.echoscript.binding.AnimationResourceBinder;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationDiagnosticMessages;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echoanimation.echoscript.face.FaceScriptCompiler;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * EchoScript syntax parser facade followed by animation-resource binding.
 */
public final class EchoScriptAnimationCompiler {
    private final EchoScriptParser parser;
    private final EchoScriptDiagnosticAdapter diagnosticAdapter;
    private final AnimationResourceBinder binder;
    private final FaceScriptCompiler faceScriptCompiler;
    private long parseCalls;
    private long bindCalls;

    public EchoScriptAnimationCompiler() {
        this(EchoScriptLanguage.parser(), new EchoScriptDiagnosticAdapter(), new AnimationResourceBinder(),
                new FaceScriptCompiler());
    }

    public EchoScriptAnimationCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            AnimationResourceBinder binder,
            FaceScriptCompiler faceScriptCompiler
    ) {
        this.parser = parser;
        this.diagnosticAdapter = diagnosticAdapter;
        this.binder = binder;
        this.faceScriptCompiler = faceScriptCompiler;
    }

    public EchoScriptAnimationCompiler(
            EchoScriptParser parser,
            EchoScriptDiagnosticAdapter diagnosticAdapter,
            AnimationResourceBinder binder
    ) {
        this(parser, diagnosticAdapter, binder, new FaceScriptCompiler());
    }

    public AnimationCompileResult compile(String sourceId, String sourceText) {
        try {
            return withSource(compileUnsafe(sourceId, sourceText), sourceId, sourceText);
        } catch (RuntimeException exception) {
            AnimationDiagnostic diagnostic = new AnimationDiagnostic("EANIM-ES-COMPILE",
                    DiagnosticSeverity.ERROR, "Не удалось скомпилировать ресурс анимации: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    List.of(AnimationDiagnosticMessages.source(diagnostic, sourceId, sourceText)));
        }
    }

    private AnimationCompileResult compileUnsafe(String sourceId, String sourceText) {
        Optional<AnimationCompileResult> faceScript = faceScriptCompiler.compile(sourceText);
        if (faceScript.isPresent()) {
            return faceScript.orElseThrow();
        }
        ParseResult parseResult;
        try {
            parseCalls++;
            parseResult = parser.parse(ParseRequest.ofV1(new SourceId(sourceId), sourceText));
        } catch (RuntimeException exception) {
            AnimationDiagnostic diagnostic = AnimationSemanticDiagnostics.error("EANIM-ES-PARSE",
                    "EchoScript source не разобран: " + exception.getMessage());
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(),
                    List.of(diagnostic));
        }
        List<AnimationDiagnostic> diagnostics = new ArrayList<>(diagnosticAdapter.adapt(parseResult.diagnostics()));
        if (!parseResult.successful() || parseResult.syntaxTree().isEmpty()) {
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
        bindCalls++;
        AnimationCompileResult bound = binder.bind(parseResult.syntaxTree().orElseThrow());
        diagnostics.addAll(bound.diagnostics());
        if (diagnostics.stream().anyMatch(AnimationDiagnostic::error)) {
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
        return new AnimationCompileResult(bound.faceRig(), bound.facePose(), bound.facialAnimation(),
                bound.animationRig(), bound.animationClip(), bound.animationDefinition(),
                bound.animationProfile(), diagnostics);
    }

    private AnimationCompileResult withSource(AnimationCompileResult result, String sourceId, String sourceText) {
        List<AnimationDiagnostic> diagnostics = result.diagnostics().stream()
                .map(diagnostic -> AnimationDiagnosticMessages.source(diagnostic, sourceId, sourceText))
                .toList();
        return new AnimationCompileResult(result.faceRig(), result.facePose(), result.facialAnimation(),
                result.animationRig(), result.animationClip(), result.animationDefinition(),
                result.animationProfile(), diagnostics);
    }

    public long parseCalls() {
        return parseCalls;
    }

    public long bindCalls() {
        return bindCalls;
    }
}
