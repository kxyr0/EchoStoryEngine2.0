package io.github.kxyr0.echotrigger.forge.api.runtime;

import io.github.kxyr0.echotrigger.forge.api.TriggerDefinitionCatalogView;
import io.github.kxyr0.echotrigger.forge.api.TriggerQueryService;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;
import java.util.Objects;

public record TriggerServiceBridge(
        TriggerService runtime,
        TriggerQueryService query,
        TriggerDefinitionCatalogView definitions,
        TriggerSubscriptionCatalogView subscriptions,
        TriggerEventIngress events) {
    public TriggerServiceBridge {
        runtime = Objects.requireNonNull(runtime, "runtime");
        query = Objects.requireNonNull(query, "query");
        definitions = Objects.requireNonNull(definitions, "definitions");
        subscriptions = Objects.requireNonNull(subscriptions, "subscriptions");
        events = Objects.requireNonNull(events, "events");
    }
}
