package io.github.kxyr0.echodialoguenpcadapter.presentation;

import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echodialoguenpcadapter.diagnostic.AdapterDiagnostics;
import io.github.kxyr0.echodialoguenpcadapter.identity.SpeakerActorIds;
import io.github.kxyr0.echodialoguenpcadapter.npc.NpcServiceAccess;
import io.github.kxyr0.echodialoguenpcadapter.npc.NpcServiceLookup;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import java.util.Objects;
import java.util.Optional;

final class NpcSpeakerPresentationSession implements SpeakerPresentationProviderSession {
    private final NpcServiceAccess serviceAccess;
    private final AdapterDiagnostics diagnostics;

    NpcSpeakerPresentationSession(NpcServiceAccess serviceAccess, AdapterDiagnostics diagnostics) {
        this.serviceAccess = Objects.requireNonNull(serviceAccess, "serviceAccess");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public SpeakerPresentationResolution resolve(SpeakerPresentationRequest request) {
        SpeakerPresentationRequest checkedRequest = Objects.requireNonNull(request, "request");
        Optional<ActorId> actorId = SpeakerActorIds.actorId(checkedRequest.speakerId());
        if (actorId.isEmpty()) {
            return SpeakerPresentationResolution.notHandled();
        }
        Optional<NpcService> service = service();
        if (service.isEmpty()) {
            return SpeakerPresentationResolution.notHandled();
        }
        try {
            return resolveQuery(checkedRequest, service.orElseThrow().query(actorId.orElseThrow()));
        } catch (RuntimeException exception) {
            diagnostics.warnOnce("EDNA-NPC-QUERY-001", "Echo NPC query завершился исключением.");
            return SpeakerPresentationResolution.failure("Echo NPC query завершился с ошибкой.");
        }
    }

    private Optional<NpcService> service() {
        NpcServiceLookup lookup;
        try {
            lookup = Objects.requireNonNull(serviceAccess.resolve(), "lookup");
        } catch (RuntimeException exception) {
            diagnostics.warnOnce("EDNA-NPC-SERVICE-001", "Echo NPC service lookup завершился исключением.");
            return Optional.empty();
        }
        return switch (lookup.status()) {
            case RESOLVED -> lookup.service();
            case UNAVAILABLE -> {
                diagnostics.warnOnce("EDNA-NPC-SERVICE-002", "Echo NPC service недоступен: " + lookup.message());
                yield Optional.empty();
            }
            case DUPLICATE_PROVIDER -> {
                diagnostics.warnOnce("EDNA-NPC-SERVICE-003",
                        "Echo NPC service имеет несколько providers: " + lookup.message());
                yield Optional.empty();
            }
        };
    }

    private SpeakerPresentationResolution resolveQuery(
            SpeakerPresentationRequest request,
            NpcResults.Query query
    ) {
        NpcResults.Query checked = Objects.requireNonNull(query, "query");
        return switch (checked.status()) {
            case FOUND -> resolveActor(request, checked.actor());
            case NOT_FOUND -> SpeakerPresentationResolution.notHandled();
            case SERVICE_STOPPED -> SpeakerPresentationResolution.temporarilyUnavailable(
                    "Echo NPC service остановлен.");
            case FAILURE -> SpeakerPresentationResolution.failure("Echo NPC query завершился с ошибкой.");
        };
    }

    private SpeakerPresentationResolution resolveActor(
            SpeakerPresentationRequest request,
            Optional<NpcActorView> actor
    ) {
        if (actor.isEmpty()) {
            return SpeakerPresentationResolution.failure("Echo NPC query вернул FOUND без actor.");
        }
        return switch (ActorStatusPolicy.decide(actor.orElseThrow())) {
            case RESOLVABLE -> SpeakerPresentationResolution.resolved(
                    NpcSpeakerPresentation.from(actor.orElseThrow(), request));
            case NOT_HANDLED -> SpeakerPresentationResolution.notHandled();
            case TEMPORARILY_UNAVAILABLE -> SpeakerPresentationResolution.temporarilyUnavailable(
                    "Echo NPC actor временно недоступен для presentation.");
            case FAILURE -> SpeakerPresentationResolution.failure(
                    "Echo NPC actor не может быть использован для presentation.");
        };
    }
}
