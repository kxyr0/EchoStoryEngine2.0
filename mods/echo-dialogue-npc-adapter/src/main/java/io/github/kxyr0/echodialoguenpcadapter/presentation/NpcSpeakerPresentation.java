package io.github.kxyr0.echodialoguenpcadapter.presentation;

import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import java.util.Objects;
import java.util.Optional;

final class NpcSpeakerPresentation {
    private NpcSpeakerPresentation() {
    }

    static ResolvedSpeakerPresentation from(NpcActorView actor, SpeakerPresentationRequest request) {
        NpcActorView checkedActor = Objects.requireNonNull(actor, "actor");
        SpeakerPresentationRequest checkedRequest = Objects.requireNonNull(request, "request");
        Optional<SpeakerVisualRef> visual = checkedActor.skinId()
                .map(skinId -> SpeakerVisualRef.skinHead(skinId.toString()))
                .or(() -> checkedRequest.authoredVisual());
        Optional<SpeakerNameColor> nameColor = checkedActor.nameColor()
                .map(color -> SpeakerNameColor.rgb(color.rgb()))
                .or(checkedRequest::authoredNameColor);
        return new ResolvedSpeakerPresentation(checkedActor.displayName(), nameColor, visual);
    }
}
