package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionDetail;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionSummary;
import java.util.List;
import java.util.Optional;

/**
 * Builds command read models from extension descriptors and registration state.
 */
final class ExtensionReadModels {
    RegisteredExtension registered(
            StoryExtensionDescriptor descriptor,
            ExtensionStatus status,
            String reason,
            StoryExtensionSession session) {
        return new RegisteredExtension(descriptor, status, reason, Optional.ofNullable(session),
                new ExtensionDetail(summary(descriptor, status), reason, actionLines(descriptor)));
    }

    private ExtensionSummary summary(StoryExtensionDescriptor descriptor, ExtensionStatus status) {
        return new ExtensionSummary(descriptor.extensionId().asString(), descriptor.displayName(),
                descriptor.implementationVersion(), descriptor.requiredApiText(), status.name(),
                descriptor.actions().size(), status == ExtensionStatus.ACCEPTED ? 0 : 1);
    }

    private List<String> actionLines(StoryExtensionDescriptor descriptor) {
        return descriptor.actions().stream()
                .map(action -> action.actionType().asString() + " recovery="
                        + action.capabilities().supportsRecovery()
                        + " cancel=" + action.capabilities().supportsCancellation()
                        + " async=" + action.capabilities().supportsAsyncCompletion())
                .toList();
    }
}
