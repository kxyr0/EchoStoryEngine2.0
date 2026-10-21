package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.forge.NavigationStructuredLogger;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolver;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolverRegistrationEvent;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolverRegistry;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackendRegistrationEvent;
import io.github.kxyr0.echonavigation.forge.persistence.NavigationSavedData;
import io.github.kxyr0.echonavigation.forge.route.ForgeSurfaceRouteService;
import io.github.kxyr0.echonavigation.forge.settings.ForgeNavigationSettingsState;
import io.github.kxyr0.echonavigation.vanilla.astar.AStarNavigationBackend;
import io.github.kxyr0.echonavigation.vanilla.VanillaNavigationBackend;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeNavigationHost {
    private final ForgeBackendRegistry backendRegistry = new ForgeBackendRegistry();
    private final List<NavigationAgentResolver> resolvers = new ArrayList<>();
    private final ForgeNavigationSettingsState settingsState;
    private final NavigationStructuredLogger logger;
    private final ForgeNavigationService service;
    private final ForgeSurfaceRouteService surfaceRoutes = new ForgeSurfaceRouteService();
    private MinecraftServer server;
    private NavigationSavedData savedData;

    public ForgeNavigationHost(Path gameDir, NavigationStructuredLogger logger) {
        this.logger = logger;
        this.settingsState = new ForgeNavigationSettingsState(gameDir);
        this.service = new ForgeNavigationService(this);
    }

    public void start(MinecraftServer startedServer) {
        server = startedServer;
        savedData = NavigationSavedData.load(startedServer);
        backendRegistry.register(new VanillaNavigationBackend());
        backendRegistry.register(new AStarNavigationBackend());
        MinecraftForge.EVENT_BUS.post(new NavigationBackendRegistrationEvent(startedServer, backendRegistry));
        MinecraftForge.EVENT_BUS.post(new NavigationAgentResolverRegistrationEvent(startedServer, this::register));
        settingsState.reload(backendRegistry.ids()).forEach(this::diagnostic);
        logger.info("navigation_started", "backends=" + backendRegistry.ids());
    }

    public void tick(long serverTick) {
        service.tick(serverTick);
    }

    public void stop() {
        flush();
        resolvers.clear();
        server = null;
        savedData = null;
    }

    public MinecraftServer server() {
        return server;
    }

    public ForgeNavigationService service() {
        return service;
    }

    public ForgeSurfaceRouteService surfaceRoutes() { return surfaceRoutes; }

    public ForgeBackendRegistry backendRegistry() {
        return backendRegistry;
    }

    List<NavigationAgentResolver> resolvers() {
        return List.copyOf(resolvers);
    }

    NavigationSettingsSnapshot settings() {
        return settingsState.current();
    }

    public ForgeNavigationSettingsState settingsState() {
        return settingsState;
    }

    public void diagnostic(NavigationDiagnostic diagnostic) {
        service.addDiagnostic(diagnostic);
        logger.diagnostic(diagnostic);
    }

    void flush() {
        if (savedData != null) {
            savedData.replaceAll(service.encodedSnapshots());
        }
    }

    private boolean register(NavigationAgentResolver resolver) {
        resolvers.add(resolver);
        return true;
    }
}
