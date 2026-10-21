package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

final class LocomotionNpcLookup {
    Optional<NpcActorView> find(MinecraftServer server, String subject) {
        ActorId actorId;
        try {
            actorId = new ActorId(subject);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        Optional<NpcService> service = service(server);
        if (service.isEmpty()) {
            return Optional.empty();
        }
        NpcResults.Query query = service.orElseThrow().query(actorId);
        return query.status() == NpcResults.QueryStatus.FOUND ? query.actor() : Optional.empty();
    }

    private Optional<NpcService> service(MinecraftServer server) {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        return lookup.status() == EchoNpcServiceLookupResult.Status.RESOLVED
                ? lookup.service() : Optional.empty();
    }
}
