package io.github.kxyr0.echodevscenarios.forge.setup;

import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort.StartResult;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceBridge;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

final class ScenarioObservationSetup {
    private final Supplier<ServerPlayer> player;

    ScenarioObservationSetup(Supplier<ServerPlayer> player) { this.player = player; }

    StartResult apply(ScenarioSetupRequest request) {
        ServerPlayer current = requirePlayer();
        var service = ObservationServiceBridge.lookup(current.getServer())
                .orElseThrow(() -> new IllegalStateException("Echo Observation unavailable"));
        String id = request.step().arguments().get(0);
        var result = request.step().operation().equals("Activate")
                ? service.activate(current.getUUID(), ObservationResourceId.parse(id))
                : service.enable(current.getUUID(), ObservationId.parse(id));
        return result.successful() ? StartResult.completed()
                : StartResult.failed(result.error().map(Enum::name).orElse("Observation mutation failed"));
    }

    private ServerPlayer requirePlayer() {
        ServerPlayer value = player.get();
        if (value == null) throw new IllegalStateException("player is unavailable");
        return value;
    }
}
