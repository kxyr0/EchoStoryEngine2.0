package io.github.kxyr0.echodevscenarios.forge.setup;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeScenarioSetup implements ScenarioSetupPort {
    private final ScenarioTimeSetup time;
    private final ScenarioActorSetup actors;
    private final ScenarioObservationSetup observations;

    public ForgeScenarioSetup(ScenarioLocationCatalog locations, Supplier<ServerPlayer> player) {
        time = new ScenarioTimeSetup(player);
        actors = new ScenarioActorSetup(locations, player);
        observations = new ScenarioObservationSetup(player);
    }

    public void bind(Completion value) { actors.bind(value); }
    public void tick() { actors.tick(); }
    public EchoInstant snapshotTime() { return time.snapshot(); }
    public void restoreTime(ScenarioSessionId owner, EchoInstant instant) { time.restore(owner, instant); }

    @Override
    public StartResult start(ScenarioSetupRequest request) {
        try {
            var step = request.step();
            if (step.service().equals("Time") && step.operation().equals("Set")) return time.set(request);
            if (step.service().equals("Player") && step.operation().equals("Place")) return actors.place(request);
            if (step.service().equals("Npc") && step.operation().equals("Ensure")) return actors.ensure(request);
            if (step.service().equals("Npc") && step.operation().equals("Form")) return actors.form(request);
            if (step.service().equals("Observation")) return observations.apply(request);
            return StartResult.failed("No public setup adapter for " + step.service() + "." + step.operation());
        } catch (RuntimeException exception) {
            return StartResult.failed("DEV_SCENARIO_SETUP_FAILED: " + exception.getMessage());
        }
    }

    @Override
    public void releaseOwner(ScenarioSessionId owner) { actors.release(owner); }

    static String token(ScenarioSetupRequest request) {
        return "echo_dev_scenarios:" + Integer.toUnsignedString(request.owner().value().hashCode(), 36)
                + ":" + request.step().id().value();
    }

    @FunctionalInterface
    public interface Completion {
        void complete(ScenarioStepId step, boolean succeeded, String detail);
    }
}
