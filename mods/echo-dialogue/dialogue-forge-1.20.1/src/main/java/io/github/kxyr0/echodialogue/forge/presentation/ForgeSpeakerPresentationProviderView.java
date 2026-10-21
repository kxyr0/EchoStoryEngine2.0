package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import java.util.List;
import java.util.Objects;

/**
 * Bounded read-only provider status exposed to commands.
 */
public record ForgeSpeakerPresentationProviderView(
        SpeakerPresentationProviderDescriptor descriptor,
        Status status,
        String reason,
        long resolved,
        long notHandled,
        long fallback,
        List<String> recentDiagnostics
) {
    public ForgeSpeakerPresentationProviderView {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        status = Objects.requireNonNull(status, "status");
        reason = Objects.requireNonNull(reason, "reason");
        recentDiagnostics = List.copyOf(Objects.requireNonNull(recentDiagnostics, "recentDiagnostics"));
    }

    public enum Status {
        ACTIVE,
        REJECTED,
        STOPPED
    }
}
