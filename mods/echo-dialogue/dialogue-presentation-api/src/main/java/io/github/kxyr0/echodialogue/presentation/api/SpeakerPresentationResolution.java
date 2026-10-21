package io.github.kxyr0.echodialogue.presentation.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit provider response. Non-NOT_HANDLED responses claim the speaker.
 */
public record SpeakerPresentationResolution(
        Kind kind,
        Optional<ResolvedSpeakerPresentation> presentation,
        String diagnostic
) {
    private static final int MAX_DIAGNOSTIC = 160;

    public SpeakerPresentationResolution {
        kind = Objects.requireNonNull(kind, "kind");
        presentation = Objects.requireNonNull(presentation, "presentation");
        diagnostic = boundedDiagnostic(diagnostic);
        if (kind == Kind.RESOLVED && presentation.isEmpty()) {
            throw new IllegalArgumentException("RESOLVED требует presentation.");
        }
        if (kind != Kind.RESOLVED && presentation.isPresent()) {
            throw new IllegalArgumentException(kind + " не должен содержать presentation.");
        }
        if (kind == Kind.NOT_HANDLED && !diagnostic.isEmpty()) {
            throw new IllegalArgumentException("NOT_HANDLED не должен содержать diagnostic.");
        }
    }

    public static SpeakerPresentationResolution notHandled() {
        return new SpeakerPresentationResolution(Kind.NOT_HANDLED, Optional.empty(), "");
    }

    public static SpeakerPresentationResolution resolved(ResolvedSpeakerPresentation presentation) {
        return new SpeakerPresentationResolution(Kind.RESOLVED, Optional.of(presentation), "");
    }

    public static SpeakerPresentationResolution temporarilyUnavailable(String diagnostic) {
        return new SpeakerPresentationResolution(Kind.TEMPORARILY_UNAVAILABLE, Optional.empty(), diagnostic);
    }

    public static SpeakerPresentationResolution failure(String diagnostic) {
        return new SpeakerPresentationResolution(Kind.FAILURE, Optional.empty(), diagnostic);
    }

    public boolean claimsSpeaker() {
        return kind != Kind.NOT_HANDLED;
    }

    public enum Kind {
        NOT_HANDLED,
        RESOLVED,
        TEMPORARILY_UNAVAILABLE,
        FAILURE
    }

    private static String boundedDiagnostic(String value) {
        String checked = Objects.requireNonNull(value, "diagnostic").trim();
        if (checked.length() <= MAX_DIAGNOSTIC) {
            return checked;
        }
        return checked.substring(0, MAX_DIAGNOSTIC);
    }
}
