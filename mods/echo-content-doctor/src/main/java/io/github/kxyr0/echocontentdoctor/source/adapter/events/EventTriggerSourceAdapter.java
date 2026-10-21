package io.github.kxyr0.echocontentdoctor.source.adapter.events;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.core.binding.TriggerDefinitionBinder;
import java.nio.file.Path;
import java.util.List;

public final class EventTriggerSourceAdapter implements SourceAdapter {
    public boolean accepts(Path path) { return path.getFileName().toString().endsWith(".trigger.es"); }
    public AdaptedSource adapt(String sourceId, String text) {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(sourceId), text));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            var problems = parsed.diagnostics().stream().map(value -> new ContentSourceProblem(ContentSeverity.ERROR,
                    "ECHO_DOCTOR_EVENT_SYNTAX", SourceIdentities.inferred(sourceId), sourceId, text,
                    value.span().map(span -> span.start().line()).orElse(1),
                    value.span().map(span -> span.start().column()).orElse(1), value.message())).toList();
            return new AdaptedSource(List.of(), problems);
        }
        var inline = new InlineEventSourceAdapter();
        if (inline.hasSections(parsed.syntaxTree().get())) {
            return inline.bind(sourceId, text, parsed.syntaxTree().get());
        }
        var bound = new TriggerDefinitionBinder().bind(parsed.syntaxTree().get(),
                io.github.kxyr0.echotrigger.core.reload.TriggerResourceFingerprint.sha256(text).value(),
                new TriggerRevision(1));
        var problems = bound.diagnostics().stream().map(value -> new ContentSourceProblem(ContentSeverity.ERROR,
                "ECHO_DOCTOR_EVENT_SUBSCRIPTION_INVALID", SourceIdentities.inferred(sourceId), sourceId, text,
                value.source().map(location -> location.line()).orElse(1),
                value.source().map(location -> location.column()).orElse(1), value.message())).toList();
        var descriptors = bound.definition().stream().map(value -> ContentDescriptor.simple(
                new ContentResourceId(value.id().value()), ContentResourceKind.TRIGGER, SourceSpans.file(sourceId, text), List.of())
                .withEventSubscriptions(value.eventSubscription().stream().toList())).toList();
        return new AdaptedSource(descriptors, problems);
    }
}
