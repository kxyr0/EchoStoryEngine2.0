package io.github.kxyr0.echocontentdoctor.source.adapter.events;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter.AdaptedSource;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotrigger.core.binding.inline.InlineEventBinder;
import io.github.kxyr0.echotrigger.core.reload.TriggerResourceFingerprint;
import java.util.ArrayList;
import java.util.List;

public final class InlineEventSourceAdapter {
    public boolean hasSections(CompilationUnitSyntax unit) {
        return new InlineEventBinder().hasDeclarations(unit.classDeclaration());
    }
    public AdaptedSource enrich(String sourceId, String text, AdaptedSource original) {
        if (!text.contains("When") || sourceId.endsWith(".trigger.es")) return original;
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(sourceId), text));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty() || !hasSections(parsed.syntaxTree().get())) return original;
        var events = bind(sourceId, text, parsed.syntaxTree().get());
        var definitions = events.descriptors().stream().flatMap(value -> value.eventSubscriptions().stream()).toList();
        var problems = new ArrayList<>(original.problems());
        problems.addAll(events.problems());
        return new AdaptedSource(original.descriptors().stream()
                .map(value -> value.withEventSubscriptions(definitions)).toList(), problems, original.migrations());
    }
    public AdaptedSource bind(String sourceId, String text, CompilationUnitSyntax unit) {
        var bound = new InlineEventBinder().bind(unit, TriggerResourceFingerprint.sha256(text).value());
        var problems = bound.diagnostics().stream().map(value -> new ContentSourceProblem(ContentSeverity.ERROR,
                "ECHO_DOCTOR_EVENT_SUBSCRIPTION_INVALID", SourceIdentities.inferred(sourceId), sourceId, text,
                value.source().map(location -> location.line()).orElse(1),
                value.source().map(location -> location.column()).orElse(1), value.message())).toList();
        List<ContentDescriptor> descriptors = bound.definitions().stream().map(value -> ContentDescriptor.simple(
                new ContentResourceId(value.subscription().id().value().replaceFirst("#When$", "")),
                ContentResourceKind.TRIGGER, SourceSpans.file(sourceId, text), List.of())
                .withEventSubscriptions(List.of(value))).toList();
        return new AdaptedSource(descriptors, problems);
    }
}
