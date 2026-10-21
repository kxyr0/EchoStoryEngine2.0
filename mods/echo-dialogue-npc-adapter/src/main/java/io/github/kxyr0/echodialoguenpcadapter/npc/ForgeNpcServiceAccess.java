package io.github.kxyr0.echodialoguenpcadapter.npc;

import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

public final class ForgeNpcServiceAccess implements NpcServiceAccess {
    private final MinecraftServer server;

    public ForgeNpcServiceAccess(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public NpcServiceLookup resolve() {
        EchoNpcServiceLookupResult result = EchoNpcServiceBridge.lookup(server);
        return switch (result.status()) {
            case RESOLVED -> NpcServiceLookup.resolved(result.service().orElseThrow());
            case UNAVAILABLE -> NpcServiceLookup.unavailable(result.message());
            case DUPLICATE_PROVIDER -> NpcServiceLookup.duplicateProvider(result.message());
        };
    }
}
