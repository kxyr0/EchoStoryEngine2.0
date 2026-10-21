package io.github.kxyr0.echocontentdoctor.model;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.fact.AliasFact;
import io.github.kxyr0.echocontentdoctor.fact.LeaseFact;
import io.github.kxyr0.echocontentdoctor.fact.ScheduleFact;
import io.github.kxyr0.echocontentdoctor.fact.TriggerFact;
import io.github.kxyr0.echocontentdoctor.fact.VoiceFact;
import io.github.kxyr0.echocontentdoctor.fact.WaitFact;
import java.util.List;
import java.util.Objects;

public record ContentDescriptor(
        ContentResourceId id,
        ContentResourceKind kind,
        SourceSpan span,
        List<ContentReference> references,
        List<ContentNode> nodes,
        List<AliasFact> aliases,
        List<ScheduleFact> schedules,
        List<WaitFact> waits,
        List<LeaseFact> leases,
        List<VoiceFact> voices,
        List<TriggerFact> triggers,
        boolean enabled,
        boolean placed,
        List<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition> eventSubscriptions
) {
    public ContentDescriptor(ContentResourceId id, ContentResourceKind kind, SourceSpan span,
            List<ContentReference> references, List<ContentNode> nodes, List<AliasFact> aliases,
            List<ScheduleFact> schedules, List<WaitFact> waits, List<LeaseFact> leases, List<VoiceFact> voices,
            List<TriggerFact> triggers, boolean enabled, boolean placed) {
        this(id, kind, span, references, nodes, aliases, schedules, waits, leases, voices, triggers, enabled, placed, List.of());
    }
    public ContentDescriptor {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        span = Objects.requireNonNull(span, "span");
        references = List.copyOf(references);
        nodes = List.copyOf(nodes);
        aliases = List.copyOf(aliases);
        schedules = List.copyOf(schedules);
        waits = List.copyOf(waits);
        leases = List.copyOf(leases);
        voices = List.copyOf(voices);
        triggers = List.copyOf(triggers);
        eventSubscriptions = List.copyOf(eventSubscriptions);
    }

    public static ContentDescriptor simple(ContentResourceId id, ContentResourceKind kind, SourceSpan span,
            List<ContentReference> references) {
        return new ContentDescriptor(id, kind, span, references, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), true, true);
    }

    public ContentDescriptor withNodes(List<ContentNode> replacement) {
        return new ContentDescriptor(id, kind, span, references, replacement, aliases, schedules, waits, leases,
                voices, triggers, enabled, placed, eventSubscriptions);
    }
    public ContentDescriptor withEventSubscriptions(List<io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition> events) {
        return new ContentDescriptor(id, kind, span, references, nodes, aliases, schedules, waits, leases, voices, triggers,
                enabled, placed, events);
    }
}
