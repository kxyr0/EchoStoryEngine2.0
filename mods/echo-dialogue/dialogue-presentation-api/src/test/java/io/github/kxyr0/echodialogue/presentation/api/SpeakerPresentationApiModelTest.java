package io.github.kxyr0.echodialogue.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpeakerPresentationApiModelTest {
    @Test
    void validatesProviderIdsAndApiVersions() {
        assertEquals("sample:provider", SpeakerPresentationProviderId.parse("sample:provider").toString());
        assertThrows(IllegalArgumentException.class, () -> SpeakerPresentationProviderId.parse("Sample:Provider"));
        assertTrue(SpeakerPresentationApiVersion.current().supports(1, 0));
        assertTrue(SpeakerPresentationApiVersion.current().supports(1, 1));
        assertTrue(SpeakerPresentationApiVersion.current().supports(1, 2));
        assertTrue(!SpeakerPresentationApiVersion.current().supports(2, 0));
        assertTrue(!SpeakerPresentationApiVersion.current().supports(1, 3));
    }

    @Test
    void descriptorDeclaresCompatibility() {
        SpeakerPresentationProviderDescriptor descriptor = new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse("sample:provider"), "Provider", "1.0", 1, 0);

        assertTrue(descriptor.compatibleWith(SpeakerPresentationApiVersion.current()));
        assertThrows(IllegalArgumentException.class, () -> new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse("sample:bad"), "", "1.0", 1, 0));
    }

    @Test
    void requestAndResolvedPresentationAreBounded() {
        SpeakerPresentationRequest request = new SpeakerPresentationRequest(
                new DialogueSessionId("session"),
                DialogueId.parse("sample:greeting"),
                SpeakerId.parse("sample:father"),
                new AudienceId("audience"),
                SpeakerPresentationRequest.Kind.LINE,
                "Отец",
                Optional.of(SpeakerNameColor.parse("#57C7FF")),
                Optional.of(SpeakerVisualRef.skinHead("sample:father")));

        assertEquals("Отец", request.authoredDisplayName());
        assertEquals(SpeakerPresentationRequest.Kind.LINE, request.kind());
        assertEquals("#57C7FF", request.authoredNameColor().orElseThrow().hex());
        assertThrows(IllegalArgumentException.class, () -> ResolvedSpeakerPresentation.textOnly(""));
        assertThrows(IllegalArgumentException.class, () -> ResolvedSpeakerPresentation.textOnly("Bad\nName"));
        ResolvedSpeakerPresentation colored = ResolvedSpeakerPresentation.textOnly(
                "РћС‚РµС†", Optional.of(SpeakerNameColor.parse("#8FD8FF")));
        assertEquals("#8FD8FF", colored.nameColor().orElseThrow().hex());
    }

    @Test
    void resolutionVariantsAreExplicitAndNonNull() {
        ResolvedSpeakerPresentation presentation = ResolvedSpeakerPresentation.textOnly("Динамический Отец");
        SpeakerPresentationResolution resolved = SpeakerPresentationResolution.resolved(presentation);

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolved.kind());
        assertTrue(resolved.presentation().isPresent());
        assertTrue(SpeakerPresentationResolution.notHandled().presentation().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new SpeakerPresentationResolution(
                SpeakerPresentationResolution.Kind.NOT_HANDLED, Optional.empty(), "ignored"));
    }
}
