package io.github.kxyr0.echoplayer.forge.dialogue;

import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderCreationContext;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderFactory;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import java.util.Objects;

final class PlayerSpeakerPresentationProviderFactory implements SpeakerPresentationProviderFactory {
    private static final String PROVIDER_ID = "echo_content_player:player_presentation";
    private static final String DISPLAY_NAME = "Echo Content Player Speaker Presentation";

    private final PlayerProfileQueryService profiles;
    private final PlayerAppearanceExtensions appearanceExtensions;
    private final SpeakerPresentationProviderDescriptor descriptor;

    PlayerSpeakerPresentationProviderFactory(
            PlayerProfileQueryService profiles,
            PlayerAppearanceExtensions appearanceExtensions,
            String implementationVersion
    ) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.appearanceExtensions = Objects.requireNonNull(appearanceExtensions, "appearanceExtensions");
        this.descriptor = new SpeakerPresentationProviderDescriptor(
                SpeakerPresentationProviderId.parse(PROVIDER_ID), DISPLAY_NAME,
                Objects.requireNonNull(implementationVersion, "implementationVersion"));
    }

    @Override
    public SpeakerPresentationProviderDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public SpeakerPresentationProviderSession create(SpeakerPresentationProviderCreationContext context) {
        return new PlayerSpeakerPresentationSession(profiles, appearanceExtensions);
    }
}
