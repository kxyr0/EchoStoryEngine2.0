package io.github.kxyr0.echoplayer.forge.dialogue;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import java.util.Objects;
import java.util.Optional;

final class PlayerSpeakerPresentation {
    private PlayerSpeakerPresentation() {
    }

    static ResolvedSpeakerPresentation from(
            PlayerProfileSnapshot profile,
            SpeakerPresentationRequest request,
            PlayerAppearanceExtensions appearanceExtensions
    ) {
        PlayerProfileSnapshot checkedProfile = Objects.requireNonNull(profile, "profile");
        SpeakerPresentationRequest checkedRequest = Objects.requireNonNull(request, "request");
        Objects.requireNonNull(appearanceExtensions, "appearanceExtensions");
        return new ResolvedSpeakerPresentation(
                checkedProfile.identity().resolvedDisplayName(),
                resolvedFaceVisual(checkedProfile, appearanceExtensions)
                        .or(() -> resourceSkinVisual(checkedProfile.appearance()))
                        .or(() -> checkedRequest.authoredVisual()));
    }

    private static Optional<SpeakerVisualRef> resolvedFaceVisual(
            PlayerProfileSnapshot profile,
            PlayerAppearanceExtensions appearanceExtensions
    ) {
        return appearanceExtensions.resolve(profile.playerId(), profile.appearance())
                .map(ResolvedPlayerAppearance::faceSkin)
                .map(SpeakerVisualRef::skinHead);
    }

    private static Optional<SpeakerVisualRef> resourceSkinVisual(PlayerAppearanceSnapshot appearance) {
        PlayerAppearanceSnapshot checked = Objects.requireNonNull(appearance, "appearance");
        if (checked.skinMode() != PlayerSkinMode.RESOURCE) {
            return Optional.empty();
        }
        return checked.skinId().map(skinId -> SpeakerVisualRef.skinHead(skinId.toString()));
    }
}
