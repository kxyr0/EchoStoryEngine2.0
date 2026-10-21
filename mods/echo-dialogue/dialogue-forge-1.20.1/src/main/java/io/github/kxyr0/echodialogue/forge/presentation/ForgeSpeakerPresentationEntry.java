package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime state and bounded counters for one provider descriptor.
 */
final class ForgeSpeakerPresentationEntry {
    private static final int RECENT_LIMIT = 5;

    private final SpeakerPresentationProviderDescriptor descriptor;
    private final Optional<SpeakerPresentationProviderSession> session;
    private final ArrayDeque<String> recentDiagnostics = new ArrayDeque<>();
    private ForgeSpeakerPresentationProviderView.Status status;
    private String reason;
    private long resolved;
    private long notHandled;
    private long fallback;

    private ForgeSpeakerPresentationEntry(
            SpeakerPresentationProviderDescriptor descriptor,
            Optional<SpeakerPresentationProviderSession> session,
            ForgeSpeakerPresentationProviderView.Status status,
            String reason
    ) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.session = Objects.requireNonNull(session, "session");
        this.status = Objects.requireNonNull(status, "status");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    static ForgeSpeakerPresentationEntry active(
            SpeakerPresentationProviderDescriptor descriptor,
            SpeakerPresentationProviderSession session
    ) {
        return new ForgeSpeakerPresentationEntry(descriptor, Optional.of(session),
                ForgeSpeakerPresentationProviderView.Status.ACTIVE, "");
    }

    static ForgeSpeakerPresentationEntry rejected(SpeakerPresentationProviderDescriptor descriptor, String reason) {
        ForgeSpeakerPresentationEntry entry = new ForgeSpeakerPresentationEntry(descriptor, Optional.empty(),
                ForgeSpeakerPresentationProviderView.Status.REJECTED, reason);
        entry.remember(reason);
        return entry;
    }

    SpeakerPresentationProviderDescriptor descriptor() {
        return descriptor;
    }

    Optional<ForgeSpeakerPresentationClaim> resolve(SpeakerPresentationRequest request) {
        if (status != ForgeSpeakerPresentationProviderView.Status.ACTIVE || session.isEmpty()) {
            return Optional.empty();
        }
        SpeakerPresentationResolution resolution = session.orElseThrow().resolve(request);
        if (resolution == null) {
            fallback++;
            remember("Provider вернул null resolution.");
            return Optional.of(new ForgeSpeakerPresentationClaim(
                    descriptor, SpeakerPresentationResolution.failure("Provider вернул null resolution.")));
        }
        if (!resolution.claimsSpeaker()) {
            notHandled++;
            return Optional.empty();
        }
        if (resolution.kind() == SpeakerPresentationResolution.Kind.RESOLVED) {
            resolved++;
        } else {
            fallback++;
            remember(resolution.diagnostic());
        }
        return Optional.of(new ForgeSpeakerPresentationClaim(descriptor, resolution));
    }

    void remember(String message) {
        recentDiagnostics.addLast(ForgeSpeakerPresentationDiagnostics.bound(message));
        while (recentDiagnostics.size() > RECENT_LIMIT) {
            recentDiagnostics.removeFirst();
        }
    }

    void close() {
        if (status != ForgeSpeakerPresentationProviderView.Status.ACTIVE) {
            return;
        }
        status = ForgeSpeakerPresentationProviderView.Status.STOPPED;
        reason = "Provider session закрыта.";
        session.ifPresent(SpeakerPresentationProviderSession::close);
    }

    ForgeSpeakerPresentationProviderView view() {
        return new ForgeSpeakerPresentationProviderView(
                descriptor, status, reason, resolved, notHandled, fallback, List.copyOf(recentDiagnostics));
    }
}
