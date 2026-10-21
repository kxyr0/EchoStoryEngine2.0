package io.github.kxyr0.echodevscenarios.runtime;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echodevscenarios.api.ManagedStateSnapshot;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionState;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.definition.MutationPolicy;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.definition.ScenarioSetupStep;
import io.github.kxyr0.echodevscenarios.port.ManagedStatePort;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echodevscenarios.port.ScenarioTargetPort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevScenarioRuntimeTest {
    @Test
    void setupUsesReceiptsAndSuppressesCrossedProductionTriggers() {
        Fixture fixture = new Fixture(true);
        ScenarioSessionId session = new ScenarioSessionId("scenario-1");

        var status = fixture.runtime.start(definition(), session);
        assertEquals(ScenarioSessionState.WAITING, status.state());
        for (int step = 0; step < 3; step++) {
            status = fixture.runtime.complete(new ScenarioStepId(step), true, "");
        }

        assertEquals(ScenarioSessionState.PLAYING, status.state());
        assertEquals("SceneExecution#17", status.targetExecution().orElseThrow());
        assertTrue(fixture.setup.requests.stream().allMatch(request ->
                request.mutationPolicy() == MutationPolicy.SETUP_NO_SIDE_EFFECTS));
        assertTrue(fixture.setup.requests.stream().allMatch(request -> request.timeCrossingPolicy().equals("SKIP_CROSSED")));
        assertEquals(List.of(session), fixture.setup.released);
    }

    @Test
    void restartRestoresBaselineAndStartsFreshWithoutTakingAnotherSnapshot() {
        Fixture fixture = new Fixture(false);
        ScenarioSessionId session = new ScenarioSessionId("scenario-2");
        fixture.runtime.start(definition(), session);

        var restarted = fixture.runtime.restart();

        assertEquals(ScenarioSessionState.PLAYING, restarted.state());
        assertEquals(1, fixture.managed.captures);
        assertEquals(1, fixture.managed.restores);
        assertEquals(2, fixture.targets.launches);
    }

    private ScenarioDefinition definition() {
        return new ScenarioDefinition(new ScenarioId("chapter1.day2.chemistry"), "Chemistry Class",
                List.of("chapter1", "day2"), List.of(
                step(0, "Time", "Set", List.of("Day(2)", "09:55")),
                step(1, "Player", "Place", List.of("jdh:university_hall")),
                step(2, "Npc", "Ensure", List.of("jdh:nolan"))),
                new ScenarioTarget(ScenarioTarget.Kind.SCENE, "jdh:chemistry_class"), span());
    }

    private ScenarioSetupStep step(int id, String service, String operation, List<String> arguments) {
        return new ScenarioSetupStep(new ScenarioStepId(id), service, operation, arguments, true, span());
    }

    private SourceSpan span() {
        SourcePosition point = new SourcePosition(0, 1, 1);
        return new SourceSpan(new SourceId("chemistry.es"), 0, 0, point, point);
    }

    private static final class Fixture {
        final Managed managed = new Managed();
        final Setup setup;
        final Targets targets = new Targets();
        final DevScenarioRuntime runtime;

        Fixture(boolean waits) {
            setup = new Setup(waits);
            runtime = new DevScenarioRuntime(() -> true, ignored -> java.util.Optional.empty(), managed, setup,
                    targets, ignored -> { });
        }
    }

    private static final class Managed implements ManagedStatePort {
        int captures;
        int restores;
        public ManagedStateSnapshot capture(ScenarioSessionId owner) {
            captures++;
            return new ManagedStateSnapshot("baseline-" + owner.value());
        }
        public void restore(ScenarioSessionId owner, ManagedStateSnapshot snapshot) { restores++; }
    }

    private static final class Setup implements ScenarioSetupPort {
        final boolean waits;
        final List<ScenarioSetupRequest> requests = new ArrayList<>();
        final List<ScenarioSessionId> released = new ArrayList<>();
        Setup(boolean waits) { this.waits = waits; }
        public StartResult start(ScenarioSetupRequest request) {
            requests.add(request);
            return waits ? StartResult.waiting("receipt-" + request.step().id().value()) : StartResult.completed();
        }
        public void releaseOwner(ScenarioSessionId owner) { released.add(owner); }
    }

    private static final class Targets implements ScenarioTargetPort {
        int launches;
        public LaunchResult launch(ScenarioSessionId owner, ScenarioTarget target) {
            launches++;
            return LaunchResult.started("SceneExecution#17");
        }
        public void stopOwned(ScenarioSessionId owner) { }
    }
}
