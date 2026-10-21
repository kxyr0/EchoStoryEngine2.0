package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable provider catalog with deterministic server-thread resolution policy.
 */
public final class ForgeSpeakerPresentationCatalog {
    private static final long SLOW_NANOS = 20_000_000L;
    private static final int DIAGNOSTIC_KEYS_LIMIT = 128;

    private final List<ForgeSpeakerPresentationEntry> entries;
    private final ForgeDiagnosticSink diagnosticSink;
    private final Set<String> emittedKeys = new LinkedHashSet<>();
    private boolean stopped;

    public ForgeSpeakerPresentationCatalog(
            List<ForgeSpeakerPresentationEntry> entries,
            ForgeDiagnosticSink diagnosticSink
    ) {
        this.entries = Objects.requireNonNull(entries, "entries").stream()
                .sorted(Comparator.comparing(entry -> entry.descriptor().id()))
                .toList();
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
    }

    public DialoguePresentation resolve(DialoguePresentation authored) {
        Objects.requireNonNull(authored, "authored");
        if (stopped) {
            emitOnce("stopped", ForgeSpeakerPresentationDiagnostics.warning(
                    "EDLG-PRESENT-014", DiagnosticCategory.PROVIDER_CATALOG_STOPPED,
                    "Provider catalog остановлен; используется authored fallback."));
            return authored;
        }
        if (entries.isEmpty() || authored.speakerId().isEmpty()) {
            return authored;
        }
        SpeakerPresentationRequest request = request(authored);
        List<ForgeSpeakerPresentationClaim> claims = collectClaims(request);
        if (claims.isEmpty()) {
            return authored;
        }
        if (claims.size() > 1) {
            ambiguity(authored, claims);
            return authored;
        }
        return resolveSingle(authored, claims.get(0));
    }

    public List<ForgeSpeakerPresentationProviderView> views() {
        return entries.stream().map(ForgeSpeakerPresentationEntry::view).toList();
    }

    public void close() {
        if (stopped) {
            return;
        }
        stopped = true;
        entries.forEach(this::closeEntry);
    }

    private List<ForgeSpeakerPresentationClaim> collectClaims(SpeakerPresentationRequest request) {
        List<ForgeSpeakerPresentationClaim> claims = new ArrayList<>();
        for (ForgeSpeakerPresentationEntry entry : entries) {
            long started = System.nanoTime();
            try {
                entry.resolve(request).ifPresent(claims::add);
            } catch (RuntimeException exception) {
                entry.remember("Provider выбросил exception: " + exception.getClass().getSimpleName());
                claims.add(new ForgeSpeakerPresentationClaim(
                        entry.descriptor(), SpeakerPresentationResolution.failure("Provider exception.")));
                emitOnce("throw|" + entry.descriptor().id(), ForgeSpeakerPresentationDiagnostics.error(
                        "EDLG-PRESENT-010", DiagnosticCategory.PROVIDER_THREW_UNEXPECTEDLY,
                        "Provider " + entry.descriptor().id()
                                + " выбросил exception; используется authored fallback."));
            }
            warnSlow(entry, System.nanoTime() - started);
        }
        return claims;
    }

    private DialoguePresentation resolveSingle(DialoguePresentation authored, ForgeSpeakerPresentationClaim claim) {
        SpeakerPresentationResolution resolution = claim.resolution();
        if (resolution.kind() == SpeakerPresentationResolution.Kind.RESOLVED) {
            ResolvedSpeakerPresentation resolved = resolution.presentation().orElseThrow();
            return authored.withSpeakerPresentation(
                    override(authored, "display") ? authored.speakerDisplayName() : resolved.displayName(),
                    override(authored, "nameColor") ? authored.speakerNameColor() : resolved.nameColor(),
                    override(authored, "visual") ? authored.speakerVisual() : resolved.visual());
        }
        emitFallback(authored, claim);
        return authored;
    }

    private boolean override(DialoguePresentation authored, String field) {
        String cueName = "speakerOverride." + field;
        return authored.cues().stream().anyMatch(cue -> cue.name().equals(cueName));
    }

    private SpeakerPresentationRequest request(DialoguePresentation authored) {
        return new SpeakerPresentationRequest(
                authored.sessionId(), authored.dialogueId(), authored.speakerId().orElseThrow(),
                authored.audienceId(), SpeakerPresentationRequest.Kind.valueOf(authored.kind().name()),
                authored.speakerDisplayName(), authored.speakerNameColor(), authored.speakerVisual());
    }

    private void emitFallback(DialoguePresentation authored, ForgeSpeakerPresentationClaim claim) {
        SpeakerPresentationResolution resolution = claim.resolution();
        DiagnosticCategory category = resolution.kind() == SpeakerPresentationResolution.Kind.TEMPORARILY_UNAVAILABLE
                ? DiagnosticCategory.PROVIDER_TEMPORARILY_UNAVAILABLE : DiagnosticCategory.PROVIDER_RESULT_MALFORMED;
        emitOnce("fallback|" + claim.descriptor().id() + "|" + authored.speakerId().orElseThrow(),
                ForgeSpeakerPresentationDiagnostics.warning("EDLG-PRESENT-012", category,
                        "Provider " + claim.descriptor().id() + " не дал presentation для "
                                + authored.speakerId().orElseThrow() + "; используется authored fallback."));
    }

    private void ambiguity(DialoguePresentation authored, List<ForgeSpeakerPresentationClaim> claims) {
        String ids = claims.stream()
                .map(claim -> claim.descriptor().id().toString())
                .sorted()
                .distinct()
                .toList()
                .toString();
        emitOnce("ambiguity|" + authored.speakerId().orElseThrow(), ForgeSpeakerPresentationDiagnostics.error(
                "EDLG-PRESENT-011", DiagnosticCategory.PROVIDER_AMBIGUITY,
                "Несколько providers заявили SpeakerId " + authored.speakerId().orElseThrow()
                        + ": " + ids + "; используется authored fallback."));
    }

    private void warnSlow(ForgeSpeakerPresentationEntry entry, long elapsedNanos) {
        if (elapsedNanos <= SLOW_NANOS) {
            return;
        }
        emitOnce("slow|" + entry.descriptor().id(), ForgeSpeakerPresentationDiagnostics.warning(
                "EDLG-PRESENT-013", DiagnosticCategory.PROVIDER_INVOCATION_SLOW,
                "Provider " + entry.descriptor().id() + " отвечал дольше soft threshold."));
    }

    private void closeEntry(ForgeSpeakerPresentationEntry entry) {
        try {
            entry.close();
        } catch (RuntimeException exception) {
            diagnosticSink.accept(ForgeSpeakerPresentationDiagnostics.error(
                    "EDLG-PRESENT-007", DiagnosticCategory.PROVIDER_SESSION_CLOSE_FAILED,
                    "Provider " + entry.descriptor().id() + " не закрыл session чисто."));
        }
    }

    private void emitOnce(String key, io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic diagnostic) {
        if (emittedKeys.size() >= DIAGNOSTIC_KEYS_LIMIT && !emittedKeys.contains(key)) {
            return;
        }
        if (emittedKeys.add(key)) {
            diagnosticSink.accept(diagnostic);
        }
    }
}
