package io.github.kxyr0.echodialogue.forge.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ForgeSpeakerPresentationSparseOverrideTest {
    @Test
    void sparseDialogueOverrideKeepsOnlyAuthoredField() {
        DialoguePresentation authored = new DialoguePresentation(
                new DialogueSessionId("session"), DialogueId.parse("sample:greeting"), new AudienceId("audience"),
                Optional.of(SpeakerId.parse("sample:father")), 7, DialoguePresentationKind.LINE, "Незнакомец",
                Optional.empty(), Optional.empty(), "Текст", false,
                List.of(new DialoguePresentation.Cue("speakerOverride.display", "true")), List.of(), true, false);
        ResolvedSpeakerPresentation current = new ResolvedSpeakerPresentation("Отец",
                Optional.of(SpeakerNameColor.parse("#8FD8FF")),
                Optional.of(SpeakerVisualRef.skinHead("sample:current")));
        var descriptor = new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse("sample:provider"), "Provider", "1.0");
        var entry = ForgeSpeakerPresentationEntry.active(descriptor,
                request -> SpeakerPresentationResolution.resolved(current));
        var catalog = new ForgeSpeakerPresentationCatalog(List.of(entry),
                new ForgeDiagnosticSink(new InMemoryDiagnosticBuffer(32)));

        DialoguePresentation result = catalog.resolve(authored);

        assertEquals("Незнакомец", result.speakerDisplayName());
        assertEquals("#8FD8FF", result.speakerNameColor().orElseThrow().hex());
        assertEquals(SpeakerVisualRef.skinHead("sample:current"), result.speakerVisual().orElseThrow());
    }
}
