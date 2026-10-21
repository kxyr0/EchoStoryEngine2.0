package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceResult;
import java.util.UUID;

final class ForgeObservationService implements ObservationService {
    private final ObservationServerRuntime runtime;

    ForgeObservationService(ObservationServerRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public ObservationServiceResult activate(UUID playerId, ObservationResourceId resourceId) {
        if (runtime.catalog().resource(resourceId).isEmpty()) {
            return ObservationServiceResult.failure(ObservationDiagnosticCode.OBS_RESOURCE_NOT_FOUND);
        }
        runtime.mutate(playerId, progress -> progress.activate(resourceId));
        return ObservationServiceResult.success();
    }

    @Override
    public ObservationServiceResult deactivate(UUID playerId, ObservationResourceId resourceId) {
        if (runtime.catalog().resource(resourceId).isEmpty()) {
            return ObservationServiceResult.failure(ObservationDiagnosticCode.OBS_RESOURCE_NOT_FOUND);
        }
        runtime.mutate(playerId, progress -> progress.deactivate(resourceId));
        return ObservationServiceResult.success();
    }

    @Override
    public ObservationServiceResult enable(UUID playerId, ObservationId observationId) {
        if (runtime.catalog().observation(observationId).isEmpty()) {
            return ObservationServiceResult.failure(ObservationDiagnosticCode.OBSERVATION_NOT_FOUND);
        }
        runtime.mutate(playerId, progress -> progress.enable(observationId));
        return ObservationServiceResult.success();
    }

    @Override
    public ObservationServiceResult disable(UUID playerId, ObservationId observationId) {
        if (runtime.catalog().observation(observationId).isEmpty()) {
            return ObservationServiceResult.failure(ObservationDiagnosticCode.OBSERVATION_NOT_FOUND);
        }
        runtime.mutate(playerId, progress -> progress.disable(observationId));
        return ObservationServiceResult.success();
    }

    @Override
    public boolean isActive(UUID playerId, ObservationResourceId resourceId) {
        return runtime.progress(playerId).active(resourceId);
    }

    @Override
    public boolean isCompleted(UUID playerId, ObservationId observationId) {
        return runtime.progress(playerId).completed(observationId);
    }
}
