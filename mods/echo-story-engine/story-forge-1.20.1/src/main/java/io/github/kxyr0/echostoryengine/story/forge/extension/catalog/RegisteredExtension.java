package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionDetail;
import java.util.Objects;
import java.util.Optional;

/**
 * One extension registration outcome and optional active session.
 */
public record RegisteredExtension(
        StoryExtensionDescriptor descriptor,
        ExtensionStatus status,
        String reason,
        Optional<StoryExtensionSession> session,
        ExtensionDetail detail) {
    public RegisteredExtension {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        status = Objects.requireNonNull(status, "status");
        reason = Objects.requireNonNull(reason, "reason");
        session = Objects.requireNonNull(session, "session");
        detail = Objects.requireNonNull(detail, "detail");
    }
}
