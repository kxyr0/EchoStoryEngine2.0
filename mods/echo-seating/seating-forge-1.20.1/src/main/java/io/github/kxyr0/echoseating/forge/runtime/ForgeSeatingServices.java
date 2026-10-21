package io.github.kxyr0.echoseating.forge.runtime;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.occupancy.SeatOccupancy;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.core.catalog.SeatZoneCatalog;
import io.github.kxyr0.echoseating.core.lifecycle.PlayerRidingTransition;
import io.github.kxyr0.echoseating.core.lifecycle.PlayerSeatingTransitionEngine;
import io.github.kxyr0.echoseating.core.lifecycle.SeatEventLedger;
import io.github.kxyr0.echoseating.core.occupancy.SeatOccupancyRegistry;
import io.github.kxyr0.echoseating.forge.api.SeatingServiceBridge;
import io.github.kxyr0.echoseating.forge.api.lifecycle.PlayerSeatingLifecycleEvent;
import io.github.kxyr0.echoseating.forge.api.view.SeatOccupancyView;
import io.github.kxyr0.echoseating.forge.api.view.SeatZoneCatalogView;
import io.github.kxyr0.echoseating.forge.api.view.SeatingDiagnosticsView;
import io.github.kxyr0.echoseating.forge.api.view.SeatingEventView;
import io.github.kxyr0.echoseating.forge.resource.SeatZoneResourceCompiler;
import io.github.kxyr0.echoseating.forge.resource.SeatZoneResourceDiscovery;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeSeatingServices implements SeatZoneCatalogView,
        SeatOccupancyView, SeatingEventView, SeatingDiagnosticsView {
    private final SeatOccupancyRegistry occupancy = new SeatOccupancyRegistry();
    private final SeatEventLedger ledger = new SeatEventLedger();
    private final List<SeatingDiagnostic> diagnostics = new ArrayList<>();
    private SeatZoneCatalog catalog = SeatZoneCatalog.empty();
    private final PlayerSeatingTransitionEngine transitions =
            new PlayerSeatingTransitionEngine(catalog, occupancy, ledger);
    private MinecraftServer server;

    public void useServer(MinecraftServer server) {
        this.server = server;
    }

    public SeatingServiceBridge bridge() {
        return new SeatingServiceBridge(this, this, this, this);
    }

    public void reload(Path worldRoot) {
        SeatZoneResourceCompiler compiler = new SeatZoneResourceCompiler();
        var result = compiler.compile(SeatZoneResourceDiscovery.discover(worldRoot));
        diagnostics.clear();
        diagnostics.addAll(result.diagnostics());
        if (result.successful()) {
            catalog = new SeatZoneCatalog(result.definitions(), catalog.revision() + 1L);
            transitions.publishCatalog(catalog);
            diagnostics.addAll(catalog.diagnostics());
        }
    }

    public List<PlayerSeatEvent> apply(PlayerRidingTransition transition) {
        List<PlayerSeatEvent> events = transitions.apply(transition);
        events.forEach(event -> MinecraftForge.EVENT_BUS.post(new PlayerSeatingLifecycleEvent(event)));
        diagnostics.addAll(transitions.diagnostics());
        return events;
    }

    @Override
    public long catalogRevision() {
        return catalog.revision();
    }

    @Override
    public List<SeatZoneDefinition> zones() {
        return catalog.zones();
    }

    @Override
    public Optional<SeatZoneDefinition> find(SeatZoneId zoneId) {
        return catalog.find(zoneId);
    }

    @Override
    public Optional<SeatOccupancy> active(PlayerRef player) {
        return occupancy.active(player);
    }

    @Override
    public List<SeatOccupancy> activeOccupancies() {
        return occupancy.activeOccupancies();
    }

    @Override
    public List<PlayerSeatEvent> events() {
        return ledger.events();
    }

    @Override
    public List<SeatingDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    public Optional<MinecraftServer> server() {
        return Optional.ofNullable(server);
    }
}
