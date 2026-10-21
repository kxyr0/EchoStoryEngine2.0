package io.github.kxyr0.echoscene.runtime;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscene.api.command.SceneCommandPort;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.definition.SceneStage;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import io.github.kxyr0.echoscene.api.port.SceneCheckpointStore;
import io.github.kxyr0.echoscene.api.port.SceneLeasePort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneRuntimeTest {
    @Test
    void parallelWaitsAndCleanupOnlyTouchesExactOwner() {
        Fixture fixture = new Fixture();
        SceneExecutionId id = new SceneExecutionId("scene-17");

        var waiting = fixture.runtime.start(fixture.parallelDefinition(), id, "story-4");
        assertEquals(SceneExecutionState.WAITING, waiting.state());
        assertEquals(2, waiting.waiting().size());
        fixture.runtime.complete(id, new SceneStepId("step_0_0"), true, "");
        var dialogue = fixture.runtime.complete(id, new SceneStepId("step_0_1"), true, "");
        assertEquals(List.of(new SceneStepId("step_2_0")), dialogue.waiting());
        var completed = fixture.runtime.complete(id, new SceneStepId("step_2_0"), true, "");

        assertEquals(SceneExecutionState.COMPLETED, completed.state());
        assertEquals(List.of(id), fixture.commands.cancelled);
        assertEquals(List.of(id), fixture.leases.released);
        assertEquals(List.of(id), fixture.cameraRestores);
    }

    @Test
    void recoveryDoesNotRepeatCompletedInstantStep() {
        Fixture first = new Fixture();
        SceneExecutionId id = new SceneExecutionId("scene-recovery");
        first.runtime.start(first.recoveryDefinition(), id, "scenario-1");
        SceneCheckpoint checkpoint = first.store.load(id).orElseThrow();
        Fixture recovered = new Fixture();

        recovered.runtime.recover(first.recoveryDefinition(), checkpoint);

        assertEquals(List.of("Play"), recovered.commands.started.stream()
                .map(request -> request.command().operation()).toList());
        assertTrue(checkpoint.completed().contains(new SceneStepId("step_0_0")));
    }

    private static final class Fixture {
        final CommandPort commands = new CommandPort();
        final LeasePort leases = new LeasePort();
        final Store store = new Store();
        final List<SceneExecutionId> cameraRestores = new ArrayList<>();
        final SceneRuntime runtime = new SceneRuntime(commands, leases, cameraRestores::add, store, ignored -> { });

        SceneDefinition parallelDefinition() {
            return definition(List.of(
                    stage(0, command("step_0_0", "father", "MoveTo", BlockingMode.DURABLE_BLOCKING,
                            capability(SceneCapabilityKind.NPC, "jdh:father")),
                            command("step_0_1", "john", "MoveTo", BlockingMode.DURABLE_BLOCKING,
                                    capability(SceneCapabilityKind.PLAYER, "jdh:john"))),
                    stage(1, command("step_1_0", "Camera", "Use", BlockingMode.INSTANT,
                            capability(SceneCapabilityKind.CAMERA, "camera"))),
                    stage(2, command("step_2_0", "Dialogue", "Play", BlockingMode.DURABLE_BLOCKING))));
        }

        SceneDefinition recoveryDefinition() {
            return definition(List.of(
                    stage(0, command("step_0_0", "Time", "Add", BlockingMode.INSTANT)),
                    stage(1, command("step_1_0", "Dialogue", "Play", BlockingMode.DURABLE_BLOCKING))));
        }
    }

    private static final class CommandPort implements SceneCommandPort {
        final List<SceneCommandRequest> started = new ArrayList<>();
        final List<SceneExecutionId> cancelled = new ArrayList<>();

        @Override
        public SceneCommandStart start(SceneCommandRequest request) {
            started.add(request);
            return request.command().blockingMode() == BlockingMode.DURABLE_BLOCKING
                    ? SceneCommandStart.waiting(request.command().stepId().value()) : SceneCommandStart.completed();
        }

        @Override
        public void cancelOwned(SceneExecutionId owner) {
            cancelled.add(owner);
        }
    }

    private static final class LeasePort implements SceneLeasePort {
        final List<SceneExecutionId> released = new ArrayList<>();

        @Override
        public LeaseResult acquire(SceneExecutionId owner, String rootOwner, SceneCapability capability) {
            return LeaseResult.granted();
        }

        @Override
        public void releaseOwned(SceneExecutionId owner) {
            released.add(owner);
        }
    }

    private static final class Store implements SceneCheckpointStore {
        private final Map<SceneExecutionId, SceneCheckpoint> values = new HashMap<>();
        public void save(SceneCheckpoint checkpoint) { values.put(checkpoint.executionId(), checkpoint); }
        public Optional<SceneCheckpoint> load(SceneExecutionId id) { return Optional.ofNullable(values.get(id)); }
    }

    private static SceneDefinition definition(List<SceneStage> stages) {
        return new SceneDefinition(new SceneResourceId("jdh:test"), new SceneLocalId("start"),
                new SceneRevision(1), stages);
    }

    private static SceneStage stage(int index, SceneCommand... commands) {
        return new SceneStage(index, List.of(commands));
    }

    private static SceneCommand command(String id, String receiver, String operation, BlockingMode mode,
            SceneCapability... capabilities) {
        return new SceneCommand(new SceneStepId(id), receiver, operation, List.of("target"), mode,
                List.of(capabilities), span());
    }

    private static SceneCapability capability(SceneCapabilityKind kind, String resource) {
        return new SceneCapability(kind, resource);
    }

    private static SourceSpan span() {
        SourcePosition point = new SourcePosition(0, 1, 1);
        return new SourceSpan(new SourceId("test.es"), 0, 0, point, point);
    }
}
