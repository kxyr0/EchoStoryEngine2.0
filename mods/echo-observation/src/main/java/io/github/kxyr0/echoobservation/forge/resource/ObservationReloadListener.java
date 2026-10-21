package io.github.kxyr0.echoobservation.forge.resource;

import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogCandidate;
import io.github.kxyr0.echoobservation.forge.runtime.ObservationServerRuntime;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ObservationReloadListener extends SimplePreparableReloadListener<ObservationCatalogCandidate> {
    private final ObservationResourceDiscovery discovery = new ObservationResourceDiscovery();
    private final ObservationCandidateCompiler compiler = new ObservationCandidateCompiler();

    @Override
    public String getName() {
        return "echo_observation_resources";
    }

    @Override
    protected ObservationCatalogCandidate prepare(ResourceManager resources, ProfilerFiller profiler) {
        return compiler.compile(discovery.discover(resources));
    }

    @Override
    protected void apply(ObservationCatalogCandidate candidate, ResourceManager resources, ProfilerFiller profiler) {
        ObservationServerRuntime.instance().install(candidate);
    }
}
