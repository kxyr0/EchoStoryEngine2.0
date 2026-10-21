package io.github.kxyr0.echovisibility.forge.runtime;

import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolution;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceBridge;
import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent;
import io.github.kxyr0.echovisibility.forge.network.ClientboundArmPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundConcealmentPacket;
import io.github.kxyr0.echovisibility.forge.network.EchoVisibilityNetwork;
import io.github.kxyr0.echovisibility.forge.network.VisibilityReportNetworkEvent;
import io.github.kxyr0.echovisibility.forge.persistence.VisibilityArmSavedData;
import io.github.kxyr0.echovisibility.forge.resource.VisibilityContentRoots;
import io.github.kxyr0.echovisibility.forge.resource.VisibilityResourceLoader;
import io.github.kxyr0.echovisibility.forge.service.ForgeVisibilityQuery;
import io.github.kxyr0.echovisibility.forge.service.ForgeVisibilityService;
import io.github.kxyr0.echovisibility.forge.service.VisibilityServerState;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class VisibilityServerHost {
    private final VisibilityServerState state;
    private final ForgeVisibilityService service;
    private final ForgeVisibilityQuery query;
    private final VisibilityReportIngress reports;
    private final VisibilityResourceLoader resources = new VisibilityResourceLoader();
    private final Path contentPackRoot;

    public VisibilityServerHost(MinecraftServer server, Path gameDirectory) {
        state = new VisibilityServerState(server);
        VisibilityArmSavedData.get(server, state);
        service = new ForgeVisibilityService(state);
        query = new ForgeVisibilityQuery(state);
        reports = new VisibilityReportIngress(state);
        contentPackRoot = new VisibilityContentRoots(gameDirectory).contentPackRoot();
    }

    public VisibilityResourceLoader.ReloadResult reload() {
        var result = resources.reload(contentPackRoot, state.catalog());
        result.diagnostics().forEach(state.diagnostics()::add);
        return result;
    }

    public void connect(ServerPlayer player) {
        state.connect(player);
        for (var snapshot : state.arms().active(VisibilityServerState.player(player))) {
            state.arms().find(snapshot.player(), snapshot.definition().id()).ifPresent(armed -> {
                VisibilityTargetResolution target = state.resolve(player, armed);
                if (target.status() == VisibilityTargetResolution.Status.RESOLVED) {
                    EchoVisibilityNetwork.send(player, new ClientboundArmPacket(armed.definition(),
                            armed.generation(), armed.connectionGeneration(), target.bounds(),
                            target.clientEntityId().orElse(-1)));
                }
            });
        }
    }

    public void disconnect(ServerPlayer player) {
        state.disconnect(player);
    }

    public void tick() {
        long tick = state.server().getTickCount();
        for (ServerPlayer player : state.server().getPlayerList().getPlayers()) {
            for (var lease : state.expire(VisibilityServerState.player(player), tick)) {
                EchoVisibilityNetwork.send(player, new ClientboundConcealmentPacket(
                        lease.id(), VisibilityConcealmentStatus.TIMED_OUT, "server timeout"));
            }
        }
    }

    public void accept(VisibilityReportNetworkEvent event) {
        if (event.player().server == state.server()) {
            reports.accept(event);
        }
    }

    public void lookup(VisibilityServiceLookupEvent event) {
        if (event.server() == state.server()) {
            event.provide(new VisibilityServiceBridge(service, query, query, query, query));
        }
    }

    public ForgeVisibilityService service() {
        return service;
    }

    public ForgeVisibilityQuery query() {
        return query;
    }
}
