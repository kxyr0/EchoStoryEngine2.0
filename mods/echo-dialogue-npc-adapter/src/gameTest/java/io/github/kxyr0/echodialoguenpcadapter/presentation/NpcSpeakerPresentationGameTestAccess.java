package io.github.kxyr0.echodialoguenpcadapter.presentation;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialoguenpcadapter.diagnostic.AdapterDiagnostics;
import io.github.kxyr0.echodialoguenpcadapter.npc.NpcServiceLookup;
import io.github.kxyr0.echonpc.api.service.NpcService;

public final class NpcSpeakerPresentationGameTestAccess {
    private NpcSpeakerPresentationGameTestAccess() {
    }

    public static SpeakerPresentationProviderSession withService(NpcService service) {
        return new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.resolved(service), new AdapterDiagnostics());
    }

    public static SpeakerPresentationProviderSession withoutService() {
        return new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.unavailable("GameTest service unavailable."), new AdapterDiagnostics());
    }
}
