package io.github.kxyr0.echodialogue.forge.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprint;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ForgeSpeakerPresentationCatalogTest {
    @Test
    void resolvedProviderReplacesOnlyPresentationFields() {
        DialoguePresentation result = catalog(session(request -> SpeakerPresentationResolution.resolved(
                new ResolvedSpeakerPresentation("Динамический Отец",
                        Optional.of(SpeakerNameColor.parse("#8FD8FF")),
                        Optional.of(SpeakerVisualRef.skinHead("sample:dynamic"))))))
                .resolve(line());

        assertEquals("Динамический Отец", result.speakerDisplayName());
        assertEquals("#8FD8FF", result.speakerNameColor().orElseThrow().hex());
        assertEquals(SpeakerVisualRef.skinHead("sample:dynamic"), result.speakerVisual().orElseThrow());
        assertEquals("Где ты был?", result.text());
        assertEquals(7, result.revision());
        assertEquals(SpeakerId.parse("sample:father"), result.speakerId().orElseThrow());
    }

    @Test
    void providerRequestCarriesAuthoredNameColor() {
        List<String> colors = new ArrayList<>();
        catalog(session(request -> {
            colors.add(request.authoredNameColor().orElseThrow().hex());
            return SpeakerPresentationResolution.notHandled();
        })).resolve(authoredColoredLine());

        assertEquals(List.of("#57C7FF"), colors);
    }

    @Test
    void fallbackCoversNoProviderUnavailableFailureExceptionAndAmbiguity() {
        assertEquals("Отец", catalog(session(request -> SpeakerPresentationResolution.notHandled()))
                .resolve(line()).speakerDisplayName());
        assertEquals("Отец", catalog(session(request ->
                SpeakerPresentationResolution.temporarilyUnavailable("нет данных"))).resolve(line())
                .speakerDisplayName());
        assertEquals("Отец", catalog(session(request ->
                SpeakerPresentationResolution.failure("ошибка provider"))).resolve(line()).speakerDisplayName());
        assertEquals("Отец", catalog(session(request -> {
            throw new IllegalStateException("boom");
        })).resolve(line()).speakerDisplayName());
        assertEquals("Отец", catalog(
                entry("sample:a", session(request -> SpeakerPresentationResolution.resolved(
                        ResolvedSpeakerPresentation.textOnly("A")))),
                entry("sample:b", session(request -> SpeakerPresentationResolution.resolved(
                        ResolvedSpeakerPresentation.textOnly("B"))))).resolve(line()).speakerDisplayName());
    }

    @Test
    void choiceKeepsChoicesAndSpeakerlessChoiceDoesNotResolve() {
        DialoguePresentation choice = new DialoguePresentation(
                new DialogueSessionId("session"),
                DialogueId.parse("sample:greeting"),
                new AudienceId("audience"),
                Optional.of(SpeakerId.parse("sample:father")), 8,
                DialoguePresentationKind.CHOICE,
                "Отец",
                Optional.empty(), Optional.empty(),
                "Выбирай.",
                false, List.of(),
                List.of(new DialoguePresentation.VisibleChoice(new ChoiceId("one"), "Первый")),
                false,
                true);
        DialoguePresentation result = catalog(session(request -> SpeakerPresentationResolution.resolved(
                ResolvedSpeakerPresentation.textOnly("Динамический")))).resolve(choice);

        assertEquals("Динамический", result.speakerDisplayName());
        assertEquals(choice.choices(), result.choices());
        assertEquals(8, result.revision());
        assertEquals("", catalog(session(request -> {
            throw new IllegalStateException("must not call");
        })).resolve(speakerlessChoice()).speakerDisplayName());
    }

    @Test
    void providerOrderIsSortedAndCloseIsIdempotent() {
        List<String> calls = new ArrayList<>();
        CountingSession b = new CountingSession("sample:b", calls);
        CountingSession a = new CountingSession("sample:a", calls);
        ForgeSpeakerPresentationCatalog catalog = catalog(entry("sample:b", b), entry("sample:a", a));

        catalog.resolve(line());
        catalog.close();
        catalog.close();

        assertEquals(List.of("sample:a", "sample:b"), calls);
        assertEquals(1, a.closeCount);
        assertEquals(1, b.closeCount);
        assertEquals("Отец", catalog.resolve(line()).speakerDisplayName());
        assertTrue(catalog.views().stream().allMatch(view -> view.status()
                == ForgeSpeakerPresentationProviderView.Status.STOPPED));
    }

    @Test
    void providerOutputIsRuntimeOnlyAndDoesNotAffectCompiledFingerprint() {
        CompiledDialogue compiled = compiledDialogue("Отец", Optional.empty());
        CompiledDialogueFingerprint original = CompiledDialogueFingerprinter.fingerprint(compiled);

        DialoguePresentation result = catalog(session(request -> SpeakerPresentationResolution.resolved(
                new ResolvedSpeakerPresentation("Динамический Отец",
                        Optional.of(SpeakerVisualRef.skinHead("sample:dynamic")))))).resolve(line());
        assertEquals("Динамический Отец", result.speakerDisplayName());
        assertEquals(original, CompiledDialogueFingerprinter.fingerprint(compiled));
        Optional<SpeakerVisualRef> dynamicVisual = Optional.of(SpeakerVisualRef.skinHead("sample:dynamic"));
        assertNotEquals(original, CompiledDialogueFingerprinter.fingerprint(
                compiledDialogue("Динамический Отец", dynamicVisual)));
    }
    private static ForgeSpeakerPresentationCatalog catalog(SpeakerPresentationProviderSession session) {
        return catalog(entry("sample:provider", session));
    }
    private static SpeakerPresentationProviderSession session(
            Function<SpeakerPresentationRequest, SpeakerPresentationResolution> resolver) {
        return resolver::apply;
    }
    private static ForgeSpeakerPresentationCatalog catalog(ForgeSpeakerPresentationEntry... entries) {
        return new ForgeSpeakerPresentationCatalog(List.of(entries),
                new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)));
    }

    private static ForgeSpeakerPresentationEntry entry(String id, SpeakerPresentationProviderSession session) {
        return ForgeSpeakerPresentationEntry.active(new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse(id), "Provider " + id, "1.0"), session);
    }

    private static DialoguePresentation line() {
        return new DialoguePresentation(
                new DialogueSessionId("session"),
                DialogueId.parse("sample:greeting"),
                new AudienceId("audience"),
                SpeakerId.parse("sample:father"),
                7,
                "Отец",
                Optional.empty(),
                "Где ты был?",
                true);
    }

    private static DialoguePresentation authoredColoredLine() {
        return new DialoguePresentation(
                new DialogueSessionId("session"),
                DialogueId.parse("sample:greeting"),
                new AudienceId("audience"),
                SpeakerId.parse("sample:player"),
                3,
                "Player",
                Optional.of(SpeakerNameColor.parse("#57C7FF")),
                Optional.empty(),
                "Where were you?",
                true);
    }

    private static CompiledDialogue compiledDialogue(String displayName, Optional<SpeakerVisualRef> visual) {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), displayName, visual,
                        "Где ты был?")
        ));
    }

    private static DialoguePresentation speakerlessChoice() {
        return DialoguePresentation.choice(
                context(),
                9,
                List.of(new DialoguePresentation.VisibleChoice(new ChoiceId("one"), "Первый")));
    }

    private static DialoguePresentation.Context context() {
        return new DialoguePresentation.Context(
                new DialogueSessionId("session"), DialogueId.parse("sample:greeting"), new AudienceId("audience"));
    }

    private static final class CountingSession implements SpeakerPresentationProviderSession {
        private final String id;
        private final List<String> calls;
        private int closeCount;

        private CountingSession(String id, List<String> calls) { this.id = id; this.calls = calls; }

        @Override
        public SpeakerPresentationResolution resolve(SpeakerPresentationRequest request) {
            calls.add(id); return SpeakerPresentationResolution.notHandled();
        }

        @Override
        public void close() { closeCount++; }
    }
}
