package io.github.kxyr0.echoplayer.forge.dialogue;

import io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerPresentationRegistrationEvent;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import java.util.Objects;
import net.minecraftforge.common.MinecraftForge;

/**
 * Optional Echo Dialogue bridge registered only when echo_dialogue is loaded.
 */
public final class PlayerDialoguePresentationRegistration {
    private final PlayerProfileQueryService profiles;
    private final PlayerAppearanceExtensions appearanceExtensions;
    private final String version;

    private PlayerDialoguePresentationRegistration(
            PlayerProfileQueryService profiles,
            PlayerAppearanceExtensions appearanceExtensions,
            String version
    ) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.appearanceExtensions = Objects.requireNonNull(appearanceExtensions, "appearanceExtensions");
        this.version = Objects.requireNonNull(version, "version");
    }

    public static void register(
            PlayerProfileQueryService profiles,
            PlayerAppearanceExtensions appearanceExtensions,
            String version
    ) {
        PlayerDialoguePresentationRegistration registration =
                new PlayerDialoguePresentationRegistration(profiles, appearanceExtensions, version);
        MinecraftForge.EVENT_BUS.addListener(registration::onRegisterSpeakerPresentation);
    }

    private void onRegisterSpeakerPresentation(EchoDialogueSpeakerPresentationRegistrationEvent event) {
        event.register(new PlayerSpeakerPresentationProviderFactory(profiles, appearanceExtensions, version));
    }
}
