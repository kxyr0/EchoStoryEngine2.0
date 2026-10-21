package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import java.util.Objects;

/**
 * One provider response that claims ownership of the requested speaker.
 */
record ForgeSpeakerPresentationClaim(
        SpeakerPresentationProviderDescriptor descriptor,
        SpeakerPresentationResolution resolution
) {
    ForgeSpeakerPresentationClaim {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        resolution = Objects.requireNonNull(resolution, "resolution");
    }
}
