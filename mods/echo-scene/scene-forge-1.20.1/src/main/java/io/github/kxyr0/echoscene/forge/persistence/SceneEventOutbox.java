package io.github.kxyr0.echoscene.forge.persistence;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.SceneFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.persistence.SavedScene;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.MinecraftServer;

final class SceneEventOutbox {
    private SceneEventOutbox() { }
    static SavedScene pin(MinecraftServer server, SavedScene scene) {
        var event = event(scene);
        if (event == null || !event.kind().durable()) return scene;
        var publication = EchoEvents.service(server).prepare(event);
        String encoded = new io.github.kxyr0.echoevents.persistence.codec.EventOutboxCodec()
                .encode(java.util.List.of(publication));
        return new SavedScene(scene.definition(), scene.checkpoint(), scene.player(), scene.committedTick(),
                Optional.of(encoded));
    }
    static void publish(MinecraftServer server, SavedScene scene, boolean recovery) {
        if (scene.eventOutbox().isPresent()) {
            var runtime = EchoEvents.service(server);
            new io.github.kxyr0.echoevents.persistence.codec.EventOutboxCodec().decode(scene.eventOutbox().get())
                    .forEach(value -> runtime.publish(value, EventMutationPolicy.NORMAL));
        } else if (!recovery && scene.checkpoint().state() == SceneExecutionState.CREATED) {
            EchoEvents.publish(server, event(scene), EventMutationPolicy.NORMAL);
        }
    }
    private static EventEnvelope<SceneFact> event(SavedScene scene) {
        SceneExecutionState state = scene.checkpoint().state();
        SceneFact.Phase phase = switch (state) {
            case CREATED -> SceneFact.Phase.STARTED;
            case COMPLETED -> SceneFact.Phase.COMPLETED;
            case CANCELLED -> SceneFact.Phase.CANCELLED;
            case FAILED -> SceneFact.Phase.FAILED;
            default -> null;
        };
        if (phase == null) return null;
        var checkpoint = scene.checkpoint();
        var source = new EventSource(new ResourceId(checkpoint.resourceId().value()),
                new ExecutionId(checkpoint.executionId().value()), new ExecutionId(checkpoint.rootOwner()),
                new TransitionId("state-" + state.name()), new EventAudience.Player(scene.player()), "echo_scene",
                scene.committedTick(), OptionalLong.empty(), Long.toString(checkpoint.revision().value()), Optional.empty());
        Optional<SymbolId> failure = phase == SceneFact.Phase.FAILED ? Optional.of(new SymbolId("SCENE_COMMAND_FAILED"))
                : Optional.empty();
        return EventEnvelope.occurrence(source, new SceneFact(phase, failure));
    }
}
