package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validates factories and creates one immutable server-scoped extension catalog.
 */
public final class ForgeExtensionCatalogBuilder {
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final ExtensionReadModels readModels = new ExtensionReadModels();

    public ForgeExtensionCatalogBuilder(
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public ExtensionCatalog build(
            List<ForgeStoryExtensionFactory> factories,
            ForgeStoryExtensionContext context) {
        List<FactoryDescriptor> descriptors = descriptors(factories);
        BuildIndexes indexes = new BuildIndexes(duplicateExtensionIds(descriptors), duplicateActionTypes(descriptors));
        BuildOutput output = new BuildOutput(new HashMap<>(), new HashMap<>());
        descriptors.forEach(descriptor -> install(descriptor, context, indexes, output));
        return new ExtensionCatalog(output.actions(), output.extensions());
    }

    private List<FactoryDescriptor> descriptors(List<ForgeStoryExtensionFactory> factories) {
        return factories.stream()
                .map(this::descriptor)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<FactoryDescriptor> descriptor(ForgeStoryExtensionFactory factory) {
        try {
            return Optional.of(new FactoryDescriptor(factory, factory.descriptor()));
        } catch (RuntimeException exception) {
            diagnosticSink.accept(diagnostics.extensionDescriptorFailed(exception.getClass().getSimpleName(),
                    clock.now()));
            return Optional.empty();
        }
    }

    private Set<NamespacedId> duplicateExtensionIds(List<FactoryDescriptor> descriptors) {
        return duplicates(descriptors.stream()
                .map(candidate -> candidate.descriptor().extensionId())
                .toList());
    }

    private Set<NamespacedId> duplicateActionTypes(List<FactoryDescriptor> descriptors) {
        return duplicates(descriptors.stream()
                .flatMap(candidate -> candidate.descriptor().actions().stream())
                .map(ActionHandlerDescriptor::actionType)
                .toList());
    }

    private Set<NamespacedId> duplicates(List<NamespacedId> values) {
        Set<NamespacedId> seen = new HashSet<>();
        return values.stream()
                .filter(value -> !seen.add(value))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void install(
            FactoryDescriptor candidate,
            ForgeStoryExtensionContext context,
            BuildIndexes indexes,
            BuildOutput output) {
        StoryExtensionDescriptor descriptor = candidate.descriptor();
        if (indexes.duplicateExtensionIds().contains(descriptor.extensionId())) {
            reject(output.extensions(), descriptor, ExtensionStatus.DUPLICATE_EXTENSION, "duplicate-extension-id");
            diagnosticSink.accept(diagnostics.duplicateExtension(descriptor.extensionId().asString(), clock.now()));
            return;
        }
        if (!descriptor.compatibleWith(StoryExtensionApiVersion.current())) {
            reject(output.extensions(), descriptor, ExtensionStatus.INCOMPATIBLE, "incompatible-api-version");
            diagnosticSink.accept(diagnostics.incompatibleExtension(descriptor.extensionId().asString(),
                    descriptor.requiredApiText(), clock.now()));
            return;
        }
        createSession(candidate, context).ifPresentOrElse(
                session -> accept(output, descriptor, session, indexes.duplicateActionTypes()),
                () -> reject(output.extensions(), descriptor, ExtensionStatus.SESSION_FAILED,
                        "session-create-failed"));
    }

    private Optional<StoryExtensionSession> createSession(
            FactoryDescriptor candidate,
            ForgeStoryExtensionContext context) {
        try {
            return Optional.of(candidate.factory().create(context));
        } catch (RuntimeException exception) {
            diagnosticSink.accept(diagnostics.extensionSessionFailed(candidate.descriptor().extensionId().asString(),
                    exception.getClass().getSimpleName(), clock.now()));
            return Optional.empty();
        }
    }

    private void accept(
            BuildOutput output,
            StoryExtensionDescriptor descriptor,
            StoryExtensionSession session,
            Set<NamespacedId> duplicateActions) {
        Map<NamespacedId, StoryActionHandler> handlers = session.handlers().stream()
                .collect(Collectors.toMap(StoryActionHandler::actionType, Function.identity(), (left, right) -> left));
        ActionInstall actionInstall = new ActionInstall(descriptor, handlers, duplicateActions);
        descriptor.actions().forEach(action -> addAction(output, actionInstall, action));
        diagnosticSink.accept(diagnostics.extensionAccepted(descriptor.extensionId().asString(),
                descriptor.actions().size(), clock.now()));
        output.extensions().put(descriptor.extensionId(), registered(descriptor, ExtensionStatus.ACCEPTED, "",
                session));
    }

    private void addAction(
            BuildOutput output,
            ActionInstall install,
            ActionHandlerDescriptor action) {
        if (install.duplicateActions().contains(action.actionType())) {
            diagnosticSink.accept(diagnostics.duplicateAction(action.actionType().asString(),
                    install.descriptor().extensionId().asString(), clock.now()));
            return;
        }
        StoryActionHandler handler = install.handlers().get(action.actionType());
        if (handler != null) {
            output.actions().put(action.actionType(), new ExtensionActionBinding(install.descriptor().extensionId(),
                    action.capabilities(), handler));
        }
    }

    private void reject(
            Map<NamespacedId, RegisteredExtension> extensions,
            StoryExtensionDescriptor descriptor,
            ExtensionStatus status,
            String reason) {
        extensions.put(descriptor.extensionId(), registered(descriptor, status, reason, null));
    }

    private RegisteredExtension registered(
            StoryExtensionDescriptor descriptor,
            ExtensionStatus status,
            String reason,
            StoryExtensionSession session) {
        return readModels.registered(descriptor, status, reason, session);
    }

    private record FactoryDescriptor(
            ForgeStoryExtensionFactory factory,
            StoryExtensionDescriptor descriptor) {
    }

    private record BuildIndexes(
            Set<NamespacedId> duplicateExtensionIds,
            Set<NamespacedId> duplicateActionTypes) {
    }

    private record BuildOutput(
            Map<NamespacedId, ExtensionActionBinding> actions,
            Map<NamespacedId, RegisteredExtension> extensions) {
    }

    private record ActionInstall(
            StoryExtensionDescriptor descriptor,
            Map<NamespacedId, StoryActionHandler> handlers,
            Set<NamespacedId> duplicateActions) {
    }
}
