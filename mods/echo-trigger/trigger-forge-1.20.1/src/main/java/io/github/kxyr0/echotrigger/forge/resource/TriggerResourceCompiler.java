package io.github.kxyr0.echotrigger.forge.resource;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.core.binding.TriggerDefinitionBinder;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echotrigger.core.reload.TriggerResourceFingerprint;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;

public final class TriggerResourceCompiler {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    public TriggerCompileResult compile(TriggerResourceText resource, TriggerRevision revision) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId(resource.path().toString()), resource.text()));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            return new TriggerCompileResult(java.util.Optional.empty(), java.util.List.of());
        }
        String fingerprint = TriggerResourceFingerprint.sha256(resource.text()).value();
        return binder.bind(parsed.syntaxTree().orElseThrow(), fingerprint, revision);
    }
}
