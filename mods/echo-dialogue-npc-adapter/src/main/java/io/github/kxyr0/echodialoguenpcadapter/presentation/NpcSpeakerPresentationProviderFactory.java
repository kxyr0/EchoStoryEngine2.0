package io.github.kxyr0.echodialoguenpcadapter.presentation;

import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderCreationContext;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderFactory;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialoguenpcadapter.diagnostic.AdapterDiagnostics;
import io.github.kxyr0.echodialoguenpcadapter.npc.ForgeNpcServiceAccess;
import java.util.Objects;

public final class NpcSpeakerPresentationProviderFactory implements SpeakerPresentationProviderFactory {
    public static final String PROVIDER_ID = "echo_dialogue_npc_adapter:npc_presentation";
    private static final String DISPLAY_NAME = "Echo NPC Speaker Presentation";

    private final SpeakerPresentationProviderDescriptor descriptor;

    public NpcSpeakerPresentationProviderFactory(String implementationVersion) {
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
        return new NpcSpeakerPresentationSession(
                new ForgeNpcServiceAccess(context.server()), new AdapterDiagnostics());
    }
}
