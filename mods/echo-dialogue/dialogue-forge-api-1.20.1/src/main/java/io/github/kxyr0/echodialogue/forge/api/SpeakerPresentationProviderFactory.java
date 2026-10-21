package io.github.kxyr0.echodialogue.forge.api;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;

/**
 * Public Forge factory registered by an external mod for one server host.
 */
public interface SpeakerPresentationProviderFactory {
    SpeakerPresentationProviderDescriptor descriptor();

    SpeakerPresentationProviderSession create(SpeakerPresentationProviderCreationContext context);
}
