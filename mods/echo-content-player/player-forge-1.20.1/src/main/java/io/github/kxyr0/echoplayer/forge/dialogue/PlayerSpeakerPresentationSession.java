package io.github.kxyr0.echoplayer.forge.dialogue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class PlayerSpeakerPresentationSession implements SpeakerPresentationProviderSession {
    private final PlayerProfileQueryService profiles;
    private final PlayerAppearanceExtensions appearanceExtensions;

    PlayerSpeakerPresentationSession(PlayerProfileQueryService profiles, PlayerAppearanceExtensions appearanceExtensions) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.appearanceExtensions = Objects.requireNonNull(appearanceExtensions, "appearanceExtensions");
    }

    @Override
    public SpeakerPresentationResolution resolve(SpeakerPresentationRequest request) {
        SpeakerPresentationRequest checked = Objects.requireNonNull(request, "request");
        if (!isAudiencePlayerSpeaker(checked.speakerId())) {
            return SpeakerPresentationResolution.notHandled();
        }
        Optional<UUID> accountUuid = audienceUuid(checked.audienceId());
        if (accountUuid.isEmpty()) {
            return SpeakerPresentationResolution.notHandled();
        }
        return profiles.findByAccountUuid(accountUuid.orElseThrow())
                .map(profile -> SpeakerPresentationResolution.resolved(
                        PlayerSpeakerPresentation.from(profile, checked, appearanceExtensions)))
                .orElseGet(() -> SpeakerPresentationResolution.temporarilyUnavailable(
                        "Echo Content Player profile is not loaded."));
    }

    static boolean isAudiencePlayerSpeaker(SpeakerId speakerId) {
        String value = Objects.requireNonNull(speakerId, "speakerId").toString();
        return value.equals("jdh:john")
                || value.equals("jdh:player")
                || value.equals("echo_content_player:player")
                || value.equals("echo_content_player:self");
    }

    private static Optional<UUID> audienceUuid(AudienceId audienceId) {
        try {
            return Optional.of(UUID.fromString(audienceId.value()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
