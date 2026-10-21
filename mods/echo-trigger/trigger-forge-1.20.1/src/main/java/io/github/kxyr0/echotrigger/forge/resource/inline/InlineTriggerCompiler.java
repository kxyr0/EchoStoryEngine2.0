package io.github.kxyr0.echotrigger.forge.resource.inline;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.policy.TriggerManualFirePolicy;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.policy.TriggerScope;
import io.github.kxyr0.echotrigger.core.binding.inline.InlineEventBinder;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.reload.TriggerResourceFingerprint;
import io.github.kxyr0.echotrigger.forge.resource.TriggerResourceText;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class InlineTriggerCompiler {
    public InlineTriggerCompilation compile(TriggerResourceText resource, TriggerRevision revision) {
        boolean triggerFile = resource.path().toString().endsWith(".trigger.es");
        if (!resource.text().contains("When")) {
            return new InlineTriggerCompilation(!triggerFile, List.of(), List.of());
        }
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId(resource.path().toString()), resource.text()));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            return new InlineTriggerCompilation(true, List.of(), parsed.diagnostics().stream().map(value ->
                    AreaDiagnosticView.withoutPosition(value.code(), resource.path() + ": " + value.message()))
                    .toList());
        }
        var unit = parsed.syntaxTree().orElseThrow();
        boolean inline = new InlineEventBinder().hasDeclarations(unit.classDeclaration());
        if (!inline) {
            return new InlineTriggerCompilation(!triggerFile, List.of(), List.of());
        }
        String fingerprint = TriggerResourceFingerprint.sha256(resource.text()).value();
        var result = new InlineEventBinder().bind(unit, fingerprint);
        var diagnostics = result.diagnostics().stream().map(value -> new AreaDiagnosticView(value.code().name(),
                resource.path() + ": " + value.message(),
                value.source().map(location -> OptionalInt.of(location.line()))
                .orElseGet(OptionalInt::empty), value.source().map(location -> OptionalInt.of(location.column()))
                .orElseGet(OptionalInt::empty))).toList();
        return new InlineTriggerCompilation(true,
                result.definitions().stream().map(definition -> executable(definition, revision)).toList(),
                diagnostics);
    }

    private CompiledTriggerDefinition executable(EventTriggerDefinition event, TriggerRevision revision) {
        String id = event.subscription().id().value().replaceFirst("#When$", "");
        return new CompiledTriggerDefinition(new TriggerId(id), TriggerScope.PLAYER, TriggerMode.REPEAT,
                revision, 0, TriggerManualFirePolicy.DENY, List.of(), Optional.empty(), Optional.empty(),
                event.subscription().revision(), Optional.of(event));
    }
}
