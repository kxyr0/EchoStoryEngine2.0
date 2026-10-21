package io.github.kxyr0.echodialogue.presentationtest;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderCreationContext;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderFactory;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.Optional;

final class EchoDialoguePresentationTestProvider implements SpeakerPresentationProviderFactory {
    private final SpeakerPresentationProviderDescriptor descriptor;
    private final boolean ambiguityOnly;

    EchoDialoguePresentationTestProvider(String providerId, boolean ambiguityOnly) {
        this.descriptor = new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse(providerId), "Echo Dialogue Presentation Test", "0.1.0");
        this.ambiguityOnly = ambiguityOnly;
    }

    @Override
    public SpeakerPresentationProviderDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public SpeakerPresentationProviderSession create(SpeakerPresentationProviderCreationContext context) {
        return this::resolve;
    }

    private SpeakerPresentationResolution resolve(SpeakerPresentationRequest request) {
        String speakerId = request.speakerId().toString();
        if (ambiguityOnly) {
            return "echo_dialogue_test:ambiguous".equals(speakerId) ? resolved("Второй Спорный") : notHandled();
        }
        return switch (speakerId) {
            case "echo_dialogue_test:dynamic_father" -> resolvedWithSkin();
            case "echo_dialogue_test:dynamic_text" -> resolved("Динамический Рассказчик");
            case "echo_dialogue_test:unavailable" ->
                    SpeakerPresentationResolution.temporarilyUnavailable("Тестовая недоступность.");
            case "echo_dialogue_test:failure" -> SpeakerPresentationResolution.failure("Тестовый сбой.");
            case "echo_dialogue_test:exception" -> throwException();
            case "echo_dialogue_test:malformed" -> null;
            case "echo_dialogue_test:ambiguous" -> resolved("Первый Спорный");
            default -> notHandled();
        };
    }

    private SpeakerPresentationResolution resolvedWithSkin() {
        return SpeakerPresentationResolution.resolved(new ResolvedSpeakerPresentation(
                "Динамический Отец", Optional.of(SpeakerVisualRef.skinHead("echo_dialogue_test:dynamic_father"))));
    }

    private SpeakerPresentationResolution resolved(String displayName) {
        return SpeakerPresentationResolution.resolved(ResolvedSpeakerPresentation.textOnly(displayName));
    }

    private SpeakerPresentationResolution notHandled() {
        return SpeakerPresentationResolution.notHandled();
    }

    private SpeakerPresentationResolution throwException() {
        throw new IllegalStateException("deterministic presentation test exception");
    }
}
