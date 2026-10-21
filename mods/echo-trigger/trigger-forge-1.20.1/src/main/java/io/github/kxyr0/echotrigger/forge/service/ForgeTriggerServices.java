package io.github.kxyr0.echotrigger.forge.service;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import java.util.function.Consumer;
import io.github.kxyr0.echotrigger.api.debug.TriggerAreaCatalogView;
import io.github.kxyr0.echotrigger.api.debug.TriggerAreaSnapshot;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.ReactiveTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.TriggerRuntimeHost;
import io.github.kxyr0.echotrigger.core.runtime.reactive.TriggerReactiveRuntime;
import io.github.kxyr0.echotrigger.forge.api.TriggerDefinitionCatalogView;
import io.github.kxyr0.echotrigger.forge.api.TriggerQueryService;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerEventIngress;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerSubscriptionCatalogView;
import java.util.List;
import java.util.Optional;

public final class ForgeTriggerServices implements TriggerService, TriggerQueryService,
        TriggerDefinitionCatalogView, TriggerAreaCatalogView, TriggerSubscriptionCatalogView, TriggerEventIngress {
    private TriggerDefinitionCatalog catalog;
    private final TriggerRuntimeHost host;
    private long catalogRevision;
    private Consumer<List<EventTriggerDefinition>> domainCatalog = ignored -> { };

    public void onDomainCatalog(Consumer<List<EventTriggerDefinition>> consumer) {
        domainCatalog = java.util.Objects.requireNonNull(consumer, "consumer");
    }

    public ForgeTriggerServices(TriggerDefinitionCatalog catalog, TriggerRuntimeHost host) {
        this.catalog = catalog;
        this.host = host;
    }

    public void publishCatalog(TriggerDefinitionCatalog catalog) {
        domainCatalog.accept(catalog.definitions().stream()
                .flatMap(value -> value.eventSubscription().stream()).toList());
        this.catalog = catalog;
        this.catalogRevision++;
        host.publishCatalog(catalog);
    }

    public long catalogRevision() {
        return catalogRevision;
    }

    @Override
    public List<EventTriggerDefinition> eventSubscriptions() {
        return catalog.definitions().stream().flatMap(value -> value.eventSubscription().stream()).toList();
    }

    public TriggerRevision nextDefinitionRevision() {
        return new TriggerRevision(catalogRevision + 1L);
    }

    public List<CompiledTriggerDefinition> definitions() {
        return catalog.definitions();
    }

    @Override
    public List<TriggerStorySubscription> storySubscriptions() {
        return catalog.storySubscriptions();
    }

    @Override
    public List<TriggerStorySubscription> storySubscriptions(TriggerEventSelector selector) {
        return catalog.storySubscriptions(selector);
    }

    @Override
    public TriggerResult start(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return host.start(id, owner, commandId);
    }

    @Override
    public TriggerResult stop(TriggerInstanceId instanceId, TriggerCommandId commandId) {
        return host.stop(instanceId, commandId);
    }

    @Override
    public TriggerResult reset(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return host.reset(id, owner, commandId);
    }

    @Override
    public TriggerResult fire(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
        return host.fire(id, owner, commandId);
    }

    @Override
    public Optional<TriggerSnapshot> query(TriggerInstanceId instanceId) {
        return host.query(instanceId);
    }

    public int clearRuntime() {
        return host.clearRuntime();
    }

    public int clearRuntime(TriggerOwnerId owner) {
        return host.clearRuntime(owner);
    }

    public int clearRuntime(TriggerId id) {
        return host.clearRuntime(id);
    }

    public int clearRuntime(TriggerId id, TriggerOwnerId owner) {
        return host.clearRuntime(id, owner);
    }

    @Override
    public List<TriggerId> triggerIds() {
        return catalog.definitions().stream().map(definition -> definition.id()).toList();
    }

    @Override
    public Optional<TriggerRevision> revision(TriggerId triggerId) {
        return catalog.find(triggerId).map(definition -> definition.revision());
    }

    public List<TriggerSnapshot> applyEvent(TriggerEvent event) {
        return host.applyEvent(event);
    }

    public List<TriggerCallbackInvocation> applyMovement(
            TriggerOwnerId owner,
            String dimension,
            Vector3 previous,
            Vector3 current,
            long serverTick
    ) {
        return host.applyMovement(owner, dimension, previous, current, serverTick);
    }

    public List<TriggerCallbackInvocation> applyMovement(TriggerReactiveRuntime.Movement movement) {
        return host.applyMovement(movement);
    }

    @Override
    public List<TriggerAreaSnapshot> areas() {
        return catalog.definitions().stream()
                .flatMap(definition -> definition.reactive().stream()
                        .map(reactive -> snapshot(definition, reactive)))
                .toList();
    }

    private static TriggerAreaSnapshot snapshot(
            CompiledTriggerDefinition definition,
            ReactiveTriggerDefinition reactive
    ) {
        return new TriggerAreaSnapshot(definition.id(), reactive.primaryArea().fieldName(),
                reactive.primaryArea().dimension(), reactive.primaryArea().shape().kind(),
                reactive.primaryArea().shape().min(), reactive.primaryArea().shape().max(),
                reactive.primaryArea().shape().center());
    }
}
